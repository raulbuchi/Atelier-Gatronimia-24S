import {
  ATELIER_MEDIA_BUCKET,
  getSupabaseClient,
  type MaterialType,
  type PortfolioPost,
  type PostVisibility,
  type SelbstkontrolleScores,
} from './supabaseClient';

export const IMAGE_COMPRESSION_DEFAULTS = {
  maxInputBytes: 10 * 1024 * 1024, // 10 MB
  acceptedMimeTypes: ['image/jpeg', 'image/png', 'image/webp'] as const,
  postMaxWidthPx: 1200,
  avatarMaxWidthPx: 400,
  jpegQuality: 0.82,
};

export interface CompressedImageResult {
  blob: Blob;
  file: File;
  previewUrl: string;
  width: number;
  height: number;
  originalSizeBytes: number;
  compressedSizeBytes: number;
  savingsPercent: number;
}

export interface CompressImageOptions {
  maxWidthPx?: number;
  quality?: number;
  maxInputBytes?: number;
}

export interface CreatePostWithPhotoInput {
  file: File;
  title: string;
  caption?: string;
  materialType: MaterialType;
  visibility: PostVisibility;
  cycleNumber?: number | null;
  weekNumber?: number | null;
  axisId?: number | null;
  recipeId?: string | null;
  recipeName?: string | null;
  isSwissmilkClassic?: boolean;
  swissCanton?: string | null;
  selbstkontrolleScores?: SelbstkontrolleScores | null;
  erkenntnisse?: string | null;
  offenePunkte?: string | null;
  aiFeedback?: string | null;
}

function generateUuid(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
}

export function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  const kb = bytes / 1024;
  if (kb < 1024) return `${kb.toFixed(0)} KB`;
  return `${(kb / 1024).toFixed(2)} MB`;
}

export function validateInputImage(
  file: File,
  maxInputBytes = IMAGE_COMPRESSION_DEFAULTS.maxInputBytes
): void {
  const accepted = IMAGE_COMPRESSION_DEFAULTS.acceptedMimeTypes as readonly string[];
  if (!accepted.includes(file.type)) {
    throw new Error(
      'Formato não suportado. Envie uma imagem JPEG, PNG ou WebP.'
    );
  }
  if (file.size > maxInputBytes) {
    throw new Error(
      `A imagem excede o limite de ${formatBytes(maxInputBytes)}.`
    );
  }
}

async function decodeImageSource(file: File): Promise<{
  source: CanvasImageSource;
  width: number;
  height: number;
  cleanup: () => void;
}> {
  if (typeof createImageBitmap === 'function') {
    try {
      const bitmap = await createImageBitmap(file, {
        imageOrientation: 'from-image',
      });
      return {
        source: bitmap,
        width: bitmap.width,
        height: bitmap.height,
        cleanup: () => bitmap.close(),
      };
    } catch {
      // Fallback para navegadores sem suporte a imageOrientation
    }
  }

  const objectUrl = URL.createObjectURL(file);
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const el = new Image();
      el.onload = () => resolve(el);
      el.onerror = () => reject(new Error('Não foi possível decodificar a imagem selecionada.'));
      el.src = objectUrl;
    });

    return {
      source: img,
      width: img.naturalWidth || img.width,
      height: img.naturalHeight || img.height,
      cleanup: () => URL.revokeObjectURL(objectUrl),
    };
  } catch (err) {
    URL.revokeObjectURL(objectUrl);
    throw err;
  }
}

/**
 * Redimensiona a imagem no navegador via HTML5 <canvas>, remove metadados EXIF
 * e converte para JPEG comprimido antes de exibir prévia ou subir ao Storage.
 */
export async function compressImageToJpeg(
  file: File,
  options: CompressImageOptions = {}
): Promise<CompressedImageResult> {
  const maxWidthPx = options.maxWidthPx ?? IMAGE_COMPRESSION_DEFAULTS.postMaxWidthPx;
  const quality = options.quality ?? IMAGE_COMPRESSION_DEFAULTS.jpegQuality;
  const maxInputBytes = options.maxInputBytes ?? IMAGE_COMPRESSION_DEFAULTS.maxInputBytes;

  validateInputImage(file, maxInputBytes);

  const decoded = await decodeImageSource(file);

  try {
    const scale = decoded.width > maxWidthPx ? maxWidthPx / decoded.width : 1;
    const targetWidth = Math.max(1, Math.round(decoded.width * scale));
    const targetHeight = Math.max(1, Math.round(decoded.height * scale));

    const canvas = document.createElement('canvas');
    canvas.width = targetWidth;
    canvas.height = targetHeight;

    const ctx = canvas.getContext('2d');
    if (!ctx) {
      throw new Error('Contexto 2D indisponível para compressão da imagem.');
    }

    // Fundo branco evita áreas pretas ao converter PNG/WebP transparentes para JPEG
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, targetWidth, targetHeight);
    ctx.imageSmoothingEnabled = true;
    ctx.imageSmoothingQuality = 'high';
    ctx.drawImage(decoded.source, 0, 0, targetWidth, targetHeight);

    const blob = await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob(
        (result) => {
          if (!result) {
            reject(new Error('Falha ao gerar JPEG comprimido.'));
            return;
          }
          resolve(result);
        },
        'image/jpeg',
        quality
      );
    });

    const baseName = file.name.replace(/\.[^.]+$/, '') || 'atelier-foto';
    const compressedFile = new File([blob], `${baseName}.jpg`, {
      type: 'image/jpeg',
      lastModified: Date.now(),
    });

    const previewUrl = URL.createObjectURL(blob);
    const savingsPercent =
      file.size > 0
        ? Math.max(0, Math.round(((file.size - blob.size) / file.size) * 100))
        : 0;

    return {
      blob,
      file: compressedFile,
      previewUrl,
      width: targetWidth,
      height: targetHeight,
      originalSizeBytes: file.size,
      compressedSizeBytes: blob.size,
      savingsPercent,
    };
  } finally {
    decoded.cleanup();
  }
}

/**
 * Converte um Blob já comprimido para base64 puro (sem prefixo data:)
 * para envio direto ao Gemini Multimodal ("Analisar Foto com o Chef IA").
 */
export async function blobToBase64(blob: Blob): Promise<string> {
  return new Promise<string>((resolve, reject) => {
    const reader = new FileReader();
    reader.onloadend = () => {
      const result = typeof reader.result === 'string' ? reader.result : '';
      const base64 = result.includes(',') ? result.split(',')[1] : result;
      if (!base64) {
        reject(new Error('Falha ao converter imagem para base64.'));
        return;
      }
      resolve(base64);
    };
    reader.onerror = () => reject(new Error('Erro ao ler os dados da imagem.'));
    reader.readAsDataURL(blob);
  });
}

/**
 * Faz upload da imagem já comprimida para `{user_id}/{subfolder}/{uuid}.jpg`
 * respeitando as políticas RLS do bucket `atelier-media`.
 */
export async function uploadToAtelierStorage(
  blob: Blob,
  userId: string,
  subfolder: 'posts' | 'avatars' = 'posts'
): Promise<{ path: string; publicUrl: string }> {
  const client = getSupabaseClient();
  if (!client) {
    throw new Error('Supabase não configurado neste ambiente.');
  }

  const filePath = `${userId}/${subfolder}/${generateUuid()}.jpg`;

  const { error: uploadError } = await client.storage
    .from(ATELIER_MEDIA_BUCKET)
    .upload(filePath, blob, {
      contentType: 'image/jpeg',
      cacheControl: '31536000',
      upsert: false,
    });

  if (uploadError) {
    throw new Error(`Erro no upload da foto: ${uploadError.message}`);
  }

  const { data } = client.storage
    .from(ATELIER_MEDIA_BUCKET)
    .getPublicUrl(filePath);

  return {
    path: filePath,
    publicUrl: data.publicUrl,
  };
}

/**
 * Fluxo completo: comprime, envia ao Storage e registra em `portfolio_posts`
 * com rollback automático no Storage caso a gravação no banco falhe.
 */
export async function createPortfolioPostWithPhoto(
  input: CreatePostWithPhotoInput,
  precompressed?: CompressedImageResult
): Promise<{ post: PortfolioPost; compression: CompressedImageResult }> {
  const client = getSupabaseClient();
  if (!client) {
    throw new Error('Supabase não configurado neste ambiente.');
  }

  const {
    data: { user },
    error: authError,
  } = await client.auth.getUser();

  if (authError || !user) {
    throw new Error('Faça login para registrar fotos no seu portfólio ou mural.');
  }

  const compression =
    precompressed ?? (await compressImageToJpeg(input.file));

  const uploaded = await uploadToAtelierStorage(
    compression.blob,
    user.id,
    'posts'
  );

  const payload = {
    user_id: user.id,
    title: input.title.trim(),
    caption: input.caption?.trim() || null,
    image_url: uploaded.publicUrl,
    image_path: uploaded.path,
    material_type: input.materialType,
    visibility: input.visibility,
    cycle_number: input.cycleNumber ?? null,
    week_number: input.weekNumber ?? null,
    axis_id: input.axisId ?? null,
    recipe_id: input.recipeId ?? null,
    recipe_name: input.recipeName ?? null,
    is_swissmilk_classic: Boolean(input.isSwissmilkClassic),
    swiss_canton: input.swissCanton ?? null,
    selbstkontrolle_scores: input.selbstkontrolleScores ?? null,
    erkenntnisse: input.erkenntnisse?.trim() || null,
    offene_punkte: input.offenePunkte?.trim() || null,
    ai_feedback: input.aiFeedback?.trim() || null,
  };

  const { data: post, error: insertError } = await client
    .from('portfolio_posts')
    .insert(payload)
    .select()
    .single();

  if (insertError || !post) {
    // Rollback para não deixar arquivo órfão no Storage
    await client.storage.from(ATELIER_MEDIA_BUCKET).remove([uploaded.path]);
    throw new Error(
      `Erro ao salvar publicação: ${insertError?.message ?? 'erro desconhecido'}`
    );
  }

  return {
    post: post as PortfolioPost,
    compression,
  };
}

/**
 * Comprime e atualiza a foto de avatar do usuário em `profiles`.
 */
export async function uploadProfileAvatar(file: File): Promise<string> {
  const client = getSupabaseClient();
  if (!client) {
    throw new Error('Supabase não configurado neste ambiente.');
  }

  const {
    data: { user },
    error: authError,
  } = await client.auth.getUser();

  if (authError || !user) {
    throw new Error('Faça login para atualizar seu avatar.');
  }

  const compressed = await compressImageToJpeg(file, {
    maxWidthPx: IMAGE_COMPRESSION_DEFAULTS.avatarMaxWidthPx,
    quality: 0.8,
  });

  try {
    const uploaded = await uploadToAtelierStorage(
      compressed.blob,
      user.id,
      'avatars'
    );

    const { error: updateError } = await client
      .from('profiles')
      .update({ avatar_url: uploaded.publicUrl, updated_at: new Date().toISOString() })
      .eq('id', user.id);

    if (updateError) {
      await client.storage.from(ATELIER_MEDIA_BUCKET).remove([uploaded.path]);
      throw new Error(`Erro ao atualizar avatar: ${updateError.message}`);
    }

    return uploaded.publicUrl;
  } finally {
    URL.revokeObjectURL(compressed.previewUrl);
  }
}

/**
 * Exclui o registro em `portfolio_posts` e remove a foto associada no Storage.
 */
export async function deletePortfolioPostAndMedia(
  postId: string,
  imagePath: string
): Promise<void> {
  const client = getSupabaseClient();
  if (!client) {
    throw new Error('Supabase não configurado neste ambiente.');
  }

  const { error: deleteError } = await client
    .from('portfolio_posts')
    .delete()
    .eq('id', postId);

  if (deleteError) {
    throw new Error(`Erro ao excluir publicação: ${deleteError.message}`);
  }

  if (imagePath) {
    await client.storage.from(ATELIER_MEDIA_BUCKET).remove([imagePath]);
  }
}
