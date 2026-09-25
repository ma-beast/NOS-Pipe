<a id="top"></a>

<p align="center">
  <img src="docs/images/NOS-Pipe-cat.png" alt="NOS-Pipe pixel cat" width="360">
</p>

[English](#english) | [Русский](#russian)

<a id="english"></a>

# NOS-Pipe 1.0.2.2

**YouTube for old computers.**

NOS-Pipe is a lightweight YouTube search and playback front end for computers that can no longer handle the modern `youtube.com` website comfortably.

It deliberately keeps the interface small and delegates playback to an external player. The application itself uses **Java 5 class format (major version 49)**, while the ready Windows package includes its own Java runtime and MPlayer.

## Which package should I use?

### Windows Installer — recommended

For ordinary Windows use, choose **`NOS-Pipe-1.0.2.2-Setup.exe`**. The installer contains the ready runtime, player, launchers, documentation and optional NOS-Pipe wallpaper helper.

Main supported Windows target: **Windows XP SP3 or newer, 32/64-bit, x86**. The project has been tested on machines as old as Pentium III-class systems.

### Portable

Choose **`NOS-Pipe-portable.zip`** if you want a self-contained Windows folder without a normal installation. Java and MPlayer are included. Start `NOS-Pipe.exe`; `RUN-NOS-Pipe.bat` is the reserve launcher and `RUN-DIAGNOSTIC.bat` enables troubleshooting logs.

### Universal — for advanced and motivated users

**`NOS-Pipe-universal-but-difficult.zip`** contains only the portable core:

- `NOS-Pipe.jar`
- `gateways.properties`
- English and Russian advanced-user guides

Bring your own **Java SE 5+ with AWT** and a suitable player/system URL handler. The guide covers experimental Windows 98/ME startup, Mac OS X Tiger PowerPC/Intel, Linux and Haiku, and explains the requirements for other unusual systems.

## What it does

- Searches YouTube through configurable third-party Invidious/Piped gateways.
- Shows a lightweight keyboard-friendly list suitable for small and unusual screens.
- First start shows popular videos; later starts can show videos related to the last successfully opened video.
- Supports bundled/local MPlayer, system URL handling and custom player commands.
- Uses a muxed 360p video+audio stream as the main compatibility mode.
- Supports manually selected resolutions with separate video/audio streams where the player and platform can handle them.
- Prefers original/ordinary audio and rejects translated or AI-dubbed tracks; if separate original audio is unavailable, it falls back to muxed playback.
- Stores only the last successfully launched video ID rather than building a viewing history.

## Controls

- `Enter` — select / confirm
- `Esc` — close Search or Settings
- Arrow keys — move through results
- Number keys — quick video selection
- `Up / Down` in MPlayer — volume

## Compatibility notes

The ready Windows package uses its bundled runtime and does not modify the system Java installation. Windows 98/ME require manual replacement with old Java 5 and MPlayer components and remain **experimental**.

NOS-Pipe has also been tested on **macOS and Linux**. The Universal package documents additional manual routes, including Tiger-era Macs and Haiku. Compatibility on unusual systems depends on the available JVM, AWT/network/TLS support and media player.

NOS-Pipe depends on third-party gateways and YouTube-side behaviour. A gateway may temporarily fail, return HTTP errors or stop supporting obsolete TLS stacks. NOS-Pipe is not a Google or YouTube product and is not supported by them.

## Source code

This repository contains the complete **NOS-Pipe 1.0.2.2 release source set**, including the Java sources, Java 5 build files, Windows launcher and wallpaper-helper sources, installer scripts/assets, retained build material and project documentation.

NOS-Pipe grew from the ideas and open source of **notPipe** by Gohoski and **JTube**, then was substantially adapted for desktop Java 5, legacy computers, MPlayer, multiple gateways, unusual display sizes and keyboard-only operation. See the included project credits and third-party notices for details.

## Related projects

- **[NecronomicOS](https://github.com/ma-beast/NecronomicOS)** — the common workshop/project hub.
- **[Naive BASIC](https://github.com/ma-beast/Naive-BASIC)** — lightweight BASIC for old Android hardware.
- **[Naive BASIC bas2apk](https://github.com/ma-beast/Naive-BASIC-bas2apk)** — BASIC → standalone Android APK.
- **[NOS-Gate](https://github.com/ma-beast/NOS-Gate)** — gateway for modern web pages and old browsers.

**Author:** Mikhail Zverev / MA-BEAST

[Русский](#russian) | [↑ Top](#top)

---

<a id="russian"></a>

# NOS-Pipe 1.0.2.2 — Русский

**YouTube для старых компьютеров.**

NOS-Pipe — лёгкая оболочка для поиска и просмотра YouTube на компьютерах, которым современный сайт `youtube.com` уже не по силам.

Интерфейс намеренно остаётся небольшим, а само воспроизведение передаётся внешнему плееру. Классы программы собраны в формате **Java 5 (major version 49)**, а готовый Windows-комплект уже содержит собственные Java и MPlayer.

## Какой пакет выбрать?

### Windows Installer — рекомендуется

Для обычной установки под Windows используйте **`NOS-Pipe-1.0.2.2-Setup.exe`**. В установщик входят готовый runtime, плеер, запускатели, документация и дополнительная утилита установки обоев NOS-Pipe.

Основная поддерживаемая Windows-конфигурация: **Windows XP SP3 или новее, 32/64-bit, x86**. Проект проверялся в том числе на компьютерах уровня Pentium III.

### Portable

**`NOS-Pipe-portable.zip`** — самодостаточная папка для Windows без обычной установки. Java и MPlayer уже внутри. Основной запуск — `NOS-Pipe.exe`; `RUN-NOS-Pipe.bat` оставлен как резервный, а `RUN-DIAGNOSTIC.bat` включает диагностический лог.

### Universal — для особо пряморуких и мотивированных

**`NOS-Pipe-universal-but-difficult.zip`** содержит только переносимое ядро:

- `NOS-Pipe.jar`
- `gateways.properties`
- подробные инструкции на русском и английском

Потребуются собственные **Java SE 5+ с AWT** и подходящий плеер либо системный обработчик URL. В инструкции разобраны экспериментальный запуск на Windows 98/ME, Mac OS X Tiger PowerPC/Intel, Linux и Haiku, а также требования для других необычных систем.

## Что умеет NOS-Pipe

- Ищет YouTube через настраиваемые сторонние шлюзы Invidious/Piped.
- Показывает лёгкий список, рассчитанный на клавиатуру, маленькие и необычные экраны.
- При первом запуске показывает популярные видео; затем может предлагать видео, связанные с последним успешно открытым.
- Работает с локальным MPlayer, системным открытием URL и пользовательской командой плеера.
- Основной режим совместимости — единый muxed-поток 360p с видео и звуком.
- При ручном выборе разрешения может использовать отдельные видео- и аудиопотоки там, где это позволяет платформа и плеер.
- Предпочитает оригинальную/обычную звуковую дорожку, отбрасывает переведённые и AI-дублированные; если отдельного оригинального звука нет, возвращается к muxed-воспроизведению.
- Хранит только ID последнего успешно запущенного видео, а не бесконечную историю просмотров.

## Управление

- `Enter` — выбрать / подтвердить
- `Esc` — закрыть Search или Settings
- Стрелки — перемещение по результатам
- Цифры — быстрый выбор видео
- `Up / Down` в MPlayer — громкость

## Совместимость

Готовый Windows-пакет использует собственный runtime и не изменяет установленную в системе Java. Для Windows 98/ME требуется вручную заменить компоненты на старые Java 5 и MPlayer; этот путь остаётся **экспериментальным**.

NOS-Pipe также проверялся на **macOS и Linux**. В Universal-пакете описаны дополнительные ручные варианты, включая старые Mac с Tiger и Haiku. На необычных системах результат зависит от конкретных JVM, AWT, сетевого/TLS-стека и медиаплеера.

NOS-Pipe зависит от сторонних шлюзов и изменений на стороне YouTube. Отдельные шлюзы могут временно переставать отвечать, возвращать HTTP-ошибки или больше не работать со старыми TLS-библиотеками. NOS-Pipe не является продуктом Google или YouTube и ими не поддерживается.

## Исходники

Репозиторий содержит полный комплект исходников релиза **NOS-Pipe 1.0.2.2**: Java-код, файлы сборки под Java 5, исходники Windows-launcher и wallpaper-helper, installer-скрипты и ресурсы, сохранённые материалы сборки и проектную документацию.

NOS-Pipe вырос из идей и открытого кода **notPipe** Gohoski и **JTube**, после чего был существенно переработан для desktop Java 5, старых компьютеров, MPlayer, нескольких шлюзов, необычных разрешений экрана и управления только клавиатурой. Подробности сохранены в файлах credits и third-party notices.

## Связанные проекты

- **[NecronomicOS](https://github.com/ma-beast/NecronomicOS)** — общая страница мастерской и проектов.
- **[Naive BASIC](https://github.com/ma-beast/Naive-BASIC)** — лёгкий BASIC для старого Android-железа.
- **[Naive BASIC bas2apk](https://github.com/ma-beast/Naive-BASIC-bas2apk)** — BASIC → самостоятельный Android APK.
- **[NOS-Gate](https://github.com/ma-beast/NOS-Gate)** — шлюз между современным вебом и старыми браузерами.

**Автор:** Михаил Зверев / MA-BEAST

[English](#english) | [↑ Наверх](#top)
