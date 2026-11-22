# Русские описания

Приложение сначала использует русские описания LostFilm и TMDB. Если сохранилось английское описание, оно переводится через собственный `/api/translate`; во время запроса показывается русское описание сериала либо состояние перевода. Английский текст не выдаётся за русский.

Сервер поддерживает DeepL при наличии ключа или локальную English → Russian модель Argos с CTranslate2 и SentencePiece. Локальный вариант не требует API-ключа и не отправляет описания внешнему переводчику. Результаты кешируются. Модель не включается в Git и APK.

На сервере установлены зависимости `pip install 'ctranslate2>=4.6,<5' 'sentencepiece>=0.2,<0.3'` и пакет `translate-en_ru-1_9` из [официального индекса Argos](https://github.com/argosopentech/argospm-index). Архив: `https://argos-net.com/v1/translate-en_ru-1_9.argosmodel`; перед распаковкой нужно проверить, что все пути остаются внутри каталога модели.

Каталог модели: `/var/lib/lostfilm-auth/models/translate-en_ru-1_9`. В нём находятся `metadata.json`, `sentencepiece.model` и `model/`. Владелец — пользователь `lostfilm`.

Файл `backend/auth_bridge/deploy/translation.conf` устанавливается в `/etc/systemd/system/lostfilm-auth.service.d/translation.conf`, затем выполняются `systemctl daemon-reload` и `systemctl restart lostfilm-auth`. Дополнительные зависимости также доступны через extra `local-translation` Python-пакета.

Проверено на production: HTTP 200, провайдер `argos`, русский результат. Память сервиса после загрузки модели — около 352 MiB, пик около 488 MiB; лимит — 1100 MiB.

Описания, уже сохранённые на английском в кеше телевизора, переводятся при показе. Очистка пользовательских данных и повторный вход не требуются.
