package org.example.ortaback.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * Загружает файл и возвращает его публичный URL.
     *
     * @param file        multipart-файл от клиента
     * @param folder      «папка» в bucket'е — обычно {@code "avatars"} или
     *                    {@code "events/<id>"} (используется как префикс к ключу)
     */
    UploadedFile upload(MultipartFile file, String folder);

    /**
     * Удаляет объект, на который указывает раннее выданный public URL.
     * Игнорирует URL'ы, не принадлежащие нашему хранилищу (best-effort).
     */
    void deleteByPublicUrl(String publicUrl);

    /**
     * Результат загрузки. {@code key} — внутренний ключ S3 (для будущих
     * операций — переименовать, удалить, выдать presigned URL).
     * {@code url} — публичный URL для прямой раздачи (попадает в
     * {@code User.avatarUrl} и т.п.).
     */
    record UploadedFile(String key, String url) {}
}
