# QR-вход на своём сервере

Android TV создаёт одноразовый QR-код. Телефон открывает браузер, пользователь входит в LostFilm, телевизор получает и проверяет сессию, затем сохраняет её в зашифрованном хранилище. Данные аккаунта проходят через ваш auth bridge, поэтому сервис должен работать по HTTPS.

## Домен и сервер

В примерах используется `auth.example.com`. Замените его на свой поддомен.

1. Создайте DNS-запись `auth.example.com`, указывающую на ваш сервер. Текущий QR-flow использует путь `/pair/<verifier>` и защищённый cookie на постоянном домене; wildcard не требуется.
2. Выпустите обычный TLS-сертификат на этот домен через HTTP challenge.
3. Установите Docker и Compose. Настройте reverse proxy для этого домена на `127.0.0.1:18015`. Передавайте исходный `Host` и `X-Forwarded-Proto: https`.
4. Из каталога `backend/auth_bridge` скопируйте `.env.example` в `.env` и задайте:

```dotenv
AUTH_BRIDGE_PUBLIC_BASE_URL=https://auth.example.com
AUTH_BRIDGE_PUBLIC_BASE_DOMAIN=auth.example.com
AUTH_BRIDGE_TRUSTED_DEVICE_COOKIE_DOMAIN=auth.example.com
AUTH_BRIDGE_TRUSTED_DEVICE_SECRET=<случайный секрет>
AUTH_BACKEND_IMAGE=lostfilm-auth-bridge:local
BACKEND_PORT=18015
```

Секрет можно создать командой `openssl rand -hex 32`. Храните `.env` на сервере с правами `600`, вне Git. Значение `AUTH_BRIDGE_TRUSTED_PROXY_IPS` должно соответствовать адресу reverse proxy, видимому контейнером. Порт 18015 публикуется только на loopback.

Соберите и запустите сервис:

```bash
docker compose up -d --build
curl --fail https://auth.example.com/health/ready
```

## APK для своего сервера

Из корня проекта:

```bash
./gradlew -PauthBridgeBaseUrl=https://auth.example.com testDebugUnitTest lint assembleDebug
```

Либо задайте `AUTH_BRIDGE_BASE_URL` в окружении сборки. Gradle-параметр имеет приоритет. Допускается только HTTPS origin без пути, query и логина/пароля. По умолчанию используется `https://lost.hagenka.online`.

В этой настройке на свой сервер переносится авторизация. Адреса существующих прокси изображений и метаданных TMDB/КиноПоиска сохраняются. Если потребуется перенести и их, настройте API-ключи и адреса отдельно по основному руководству [установки](auth-bridge-server-install.md).

## Приёмка

- Откройте «Войти» на телевизоре: QR-код должен появиться автоматически.
- Отсканируйте его на телефоне. Код на телефоне должен совпасть с кодом на телевизоре.
- Завершите вход: приложение на телевизоре должно закрыть экран авторизации, избранное — загрузиться.
- Проверьте истёкший код, повторное получение кода, отмену кнопкой «Назад» и выход из аккаунта.

Для проверки реального входа нужны доступный сервер, рабочий HTTPS на домене авторизации и аккаунт LostFilm. Локальные тесты проверяют протокол с подменённым upstream.

## Установленный сервер lost.hagenka.online

Сервис работает на `94.249.239.205` (Ubuntu 24.04) под отдельным пользователем `lostfilm` через systemd. Python-окружение: `/opt/lostfilm-auth/venv`, исходники: `/opt/lostfilm-auth/app`, конфигурация с секретом: `/etc/lostfilm-auth.env` (root:lostfilm, 0640), постоянные данные: `/var/lib/lostfilm-auth`.

Файлы развёртывания: [systemd unit](../backend/auth_bridge/deploy/lostfilm-auth.service), [Nginx](../backend/auth_bridge/deploy/nginx.conf). Сервис слушает только `127.0.0.1:18015`. Nginx обслуживает HTTPS; Certbot автоматически продлевает сертификат и перезагружает Nginx через deploy hook.

```bash
systemctl status lostfilm-auth nginx
journalctl -u lostfilm-auth --since '10 minutes ago'
curl --fail https://lost.hagenka.online/health/ready
```

Uvicorn запущен с одним worker: незавершённые QR-сессии хранятся в памяти. При перезапуске сервиса нужно получить новый QR-код. Сохранённые доверенные устройства находятся в SQLite. HTTP access logs отключены, чтобы ссылки с одноразовыми кодами не попадали в журналы веб-сервера.
