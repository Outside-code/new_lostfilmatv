# Оформление LostFilm TV

Выбраны пользователем: угловатая неоновая надпись LOST FILM для главного экрана и монограмма LF с электрическим синим и фиолетовым контуром внутри интерфейса. Золотых акцентов в интерфейсе нет.

Исходники для приложения:

- `app/src/main/res/drawable-nodpi/lostfilm_wordmark.png` — прозрачная надпись;
- `app/src/main/res/drawable-nodpi/ic_lf_logo.png` — прозрачный значок;
- `app/src/main/res/drawable-nodpi/lostfilm_launcher_foreground.png` и `lostfilm_launcher_banner.png` — значок и баннер запуска LOST / FILM с красной плашкой, выбранные пользователем по установленному на его телевизоре LostFilm.TV;
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — адаптивный значок; подпись приложения — `LostFilm.TV`.

Концепции и прозрачные версии созданы встроенным imagegen. Для прозрачных версий использовалась инструкция: удалить только чёрный фон, сохранить точные формы букв, синие грани, фиолетовые акценты и свечение; не менять дизайн. Промпты сохранены в `branding-prompts.json`.
