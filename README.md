# Учебное Android-приложение «Музыкальный плеер»

Проект на Kotlin, соответствующий требованиям из `МДК 01.03 Требования к конечному продукту всех лр.docx`.

## Что реализовано

| Требование | Где реализовано |
|---|---|
| Kotlin, Android Studio 2025.2.1.7, minSdk 26 | `app/build.gradle.kts`, `AndroidManifest.xml` |
| 3 экрана с навигацией | `MainFragment`, `TrackFragment`, `FavoritesFragment`, `nav_graph.xml`, `BottomNavigationView` |
| Воспроизведение локальных mp3/wav, Play/Pause/Stop, prev/next | `PlaybackService` (MediaPlayer, foreground service) + `MainViewModel.next/previous` |
| Сохранение текущего трека при повороте экрана | сервис + `MainViewModel` (StateFlow) |
| Room: треки, путь, название, исполнитель, лайк, обложка | `TrackEntity`, `TrackDao`, `AppDatabase` |
| Обложки | `ic_album_art`, путь обложки хранится в БД |
| Лайки и избранное, сохранение в БД | `setLike`, `FavoritesFragment` |
| Добавление своих аудиофайлов | кнопка «+» на главном экране (SAF `OpenDocument`) |
| Поворот экрана / повторное открытие экрана | состояние плеера живёт в сервисе, данные — в Room |
| Читаемая структура пакетов | `data.db`, `data.repository`, `player`, `ui` |

Демонстрационных звуков больше нет — коллекция изначально пустая. Добавьте свои mp3/wav кнопкой «+» (или кнопкой в пустом состоянии): файл копируется в приватное хранилище, а путь сохраняется в Room. Если раньше стояла старая версия с демо-треками, база данных обновится до версии 2 и старые записи исчезнут.

## Обновлённый интерфейс

- Приветственный блок с градиентом, счётчиком треков и кнопкой **«Слушать всё»**.
- Пустые состояния с пояснениями и кнопкой добавления файла.
- Карточки треков с округлыми обложками; играющий трек подсвечивается, его кнопка превращается в паузу.
- Экран трека: большая обложка, перемотка, предыдущий/следующий трек, стоп и одна кнопка лайка.
- Мини-плеер с предыдущим/следующим треком; нажатие по нему открывает детали.
- Тёмная тема: цвета и стили определены в `values/styles.xml` и `values-night/styles.xml`.
- Анимации: плавный переход между экранами, появление карточек списка, отклик кнопок нажатием, плавная смена обложки и появление мини-плеера (`res/anim`, `UiExt.kt`).
- Все размеры, отступы и радиусы вынесены в `res/values/dimens.xml`: обложки и кнопки одинакового размера, текст и иконки выровнены по центру.

## Сборка

Проект настроен под Android Studio 2025.2.1.7 (AGP 9.5.2, Gradle 9.6.0, встроенная поддержка Kotlin).

Из Android Studio: **File → Open → /home/editwe/Work/MusicPlayer**, затем **Run ▶**.

Из терминала:

```bash
cd /home/editwe/Work/MusicPlayer
JAVA_HOME=/opt/android-studio/jbr ./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

> `gradle.properties` содержит `android.disallowKotlinSourceSets=false` — это необходимо, чтобы KSP (Room) работал со встроенным Kotlin в AGP 9.

## Запуск на телефоне

1. На телефоне: **Настройки → О телефоне → 7 раз нажмите «Номер сборки»**.
2. **Настройки → Параметры разработчика → включите «Отладка по USB»**.
3. Подключите телефон USB-кабелем и подтвердите запрос RSA на экране телефона.
4. Проверьте подключение:

   ```bash
   ~/Android/Sdk/platform-tools/adb devices
   ```

   В списке должен появиться номер устройства со статусом `device`.

   Чтобы не писать полный путь каждый раз, добавьте в `~/.bashrc`:

   ```bash
   export PATH="$PATH:$HOME/Android/Sdk/platform-tools"
   ```

5. Установите APK:

   ```bash
   ./scripts/install-apk.sh
   ```

   Скрипт сам соберёт проект, если APK ещё нет, и установит его на все подключённые устройства.

Для Android 11+ можно подключиться по Wi‑Fi без кабеля:

```bash
# на телефоне: Параметры разработчика -> Беспроводная отладка -> Использовать QR-код / сопряжение
~/Android/Sdk/platform-tools/adb pair <host>:<pair_port>
~/Android/Sdk/platform-tools/adb connect <host>:<port>
./scripts/install-apk.sh
```

## Запуск в эмуляторе

AVD `Medium_Phone_API_37.0` уже создан. Запуск + установка приложения:

```bash
./scripts/start-emulator.sh
```

Скрипт ждёт полной загрузки Android (`sys.boot_completed=1`) и затем вызывает `install-apk.sh`.

### Важно про эмулятор на этой машине

Сейчас на этой машине **нет `/dev/kvm`** (виртуализация VT‑x/AMD‑V отключена в BIOS), поэтому x86_64-эмулятор не стартует:

```
x86_64 emulation currently requires hardware acceleration!
```

Чтобы эмулятор заработал:

1. Перезагрузитесь и в BIOS/UEFI включите:
   - Intel: **VT-x / Intel Virtualization Technology**
   - AMD: **SVM / AMD-V**
2. После загрузки проверьте:

   ```bash
   ls -l /dev/kvm
   ```

3. Снова запустите `./scripts/start-emulator.sh`.

Если включить виртуализацию нельзя — используйте физический телефон по инструкции выше. Это самый быстрый и надёжный способ проверить приложение.

## Структура

```text
app/src/main/java/com/example/musicplayer/
├── data/
│   ├── db/            # Room: entity, DAO, database
│   └── repository/    # импорт файлов, лайки
├── player/            # PlaybackService (MediaPlayer) и PlaybackController
├── ui/                # MainActivity, MainViewModel, адаптер и 3 экрана
└── MusicPlayerApp.kt
```
