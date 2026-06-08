# MVC Architecture Diagram - Fonos_Group13

Project hien tai theo kieu MVC gan dung cho Android Java:

- Model: `model/*` va `data/*`, gom entity/domain object, repository, Firebase, file download.
- View: `res/layout*/*`, drawable, string/resource UI.
- Controller: cac `Activity`, vi Activity bind view, bat su kien, dieu huong, goi repository va cap nhat UI.

```mermaid
flowchart TB
    subgraph V["View layer"]
        XML["XML layouts\nactivity_login, activity_register,\nactivity_discover, activity_library,\nactivity_search, activity_profile,\nactivity_reader"]
        UIRes["UI resources\ndrawable, colors, strings, themes"]
    end

    subgraph C["Controller layer"]
        Main["MainActivity\nentry + route by auth state"]
        AuthScreens["LoginActivity / RegisterActivity\nauth form + validation"]
        BrowseScreens["DiscoverActivity / SearchActivity / LibraryActivity / ProfileActivity\nbrowse, navigation, profile, library filters"]
        Reader["ActivityReader\nread book, play audio, download audio, save progress"]
    end

    subgraph M["Model + Data layer"]
        Models["Book\nUserProgress"]
        AuthRepo["AuthRepository"]
        BookRepo["BookRepository"]
        ProgressRepo["ProgressRepository"]
        DownloadRepo["DownloadedAudioRepository"]
        AudioResolver["AudioSourceResolver"]
        FirebaseConfig["FirebaseConfig"]
    end

    subgraph External["External / platform APIs"]
        FirebaseAuth["Firebase Authentication"]
        Firestore["Cloud Firestore"]
        HttpUrl["HttpURLConnection\nremote audioUrl MP3"]
        InternalFiles["App internal files\n/files/audiobooks/*.mp3"]
        Exo["Media3 ExoPlayer"]
    end

    XML --> Main
    XML --> AuthScreens
    XML --> BrowseScreens
    XML --> Reader
    UIRes --> XML

    Main --> AuthRepo
    AuthScreens --> AuthRepo
    BrowseScreens --> BookRepo
    BrowseScreens --> ProgressRepo
    BrowseScreens --> DownloadRepo
    Reader --> BookRepo
    Reader --> ProgressRepo
    Reader --> DownloadRepo
    Reader --> AudioResolver
    Reader --> Exo

    AuthRepo --> FirebaseConfig
    BookRepo --> FirebaseConfig
    ProgressRepo --> FirebaseConfig
    AuthRepo --> FirebaseAuth
    AuthRepo --> Firestore
    BookRepo --> Firestore
    ProgressRepo --> FirebaseAuth
    ProgressRepo --> Firestore

    BookRepo --> Models
    ProgressRepo --> Models
    DownloadRepo --> Models
    AudioResolver --> Models
    DownloadRepo --> HttpUrl
    DownloadRepo --> InternalFiles
    AudioResolver --> InternalFiles
    AudioResolver --> HttpUrl
```

## Audio download and playback flow

```mermaid
sequenceDiagram
    participant User
    participant Reader as ActivityReader
    participant Resolver as AudioSourceResolver
    participant DownloadRepo as DownloadedAudioRepository
    participant Player as ExoPlayer
    participant Store as Internal files / remote URL

    Reader->>Resolver: resolve(book)
    Resolver->>DownloadRepo: getDownloadedUri(bookId)
    Resolver->>Store: fallback to audioUrl
    Resolver-->>Reader: Uri
    Reader->>Player: setMediaItem(Uri) + prepare()
    User->>Reader: tap Play/Pause/Seek/Speed
    Reader->>Player: play(), pause(), seekTo(), setPlaybackParameters()

    User->>Reader: tap Download
    Reader->>DownloadRepo: download(book, callback)
    DownloadRepo->>Store: HttpURLConnection(audioUrl)
    DownloadRepo->>Store: save to app files/audiobooks/bookId.mp3
    DownloadRepo-->>Reader: callback success/error
    Reader->>Resolver: resolve(book) again
    Reader->>Player: play local downloaded file
```

## Activities

| Activity | Vai tro |
|---|---|
| `MainActivity` | Launcher entry; kiem tra user da dang nhap chua, sau do dieu huong sang `LoginActivity` hoac `DiscoverActivity`. |
| `LoginActivity` | Man hinh dang nhap, validate email/password, goi `AuthRepository.signIn`. |
| `RegisterActivity` | Man hinh tao tai khoan, validate form, goi `AuthRepository.register`. |
| `DiscoverActivity` | Trang kham pha sach/audio book, load danh sach tu `BookRepository`, mo `ActivityReader` khi chon sach. |
| `SearchActivity` | Man hinh search/navigation; hien tai chu yeu set layout va bottom navigation. |
| `LibraryActivity` | Thu vien: filter Listening/Downloaded/Finished, lay progress, kiem tra audio da download. |
| `ProfileActivity` | Hien thi thong tin Firebase user va dang xuat. |
| `ActivityReader` | Man doc/nghe sach: hien noi dung, play/pause/seek/speed, download audio, restore/save progress. |

## Services, receiver, provider

| Component | Co trong project? | Ghi chu |
|---|---:|---|
| Android `Service` | Khong | Manifest khong khai bao `<service>` va source khong co class `extends Service`. |
| Foreground/music playback service | Khong | Audio duoc phat bang `ExoPlayer` truc tiep trong `ActivityReader`, nen nhac khong chay qua `MediaSessionService`/foreground service rieng. |
| Download service / `DownloadManager` | Khong | Download MP3 duoc tu code trong `DownloadedAudioRepository`: tao `new Thread`, dung `HttpURLConnection`, luu vao internal files. |
| `BroadcastReceiver` | Khong | Manifest khong co `<receiver>`, source khong co `BroadcastReceiver`, `registerReceiver`, `sendBroadcast`. |
| `ContentProvider` | Khong | Manifest khong co `<provider>`, source khong co class `ContentProvider`. Neu y ban la "broadcast provider" thi Android khong co component ten nay; component tuong ung thuong la `ContentProvider`. |

## External services/libraries actually used

- Firebase Authentication: dang nhap, dang ky, lay current user, dang xuat.
- Cloud Firestore: `books`, `users`, `users/{uid}/progress`.
- Media3 ExoPlayer: phat audio trong `ActivityReader`.
- `HttpURLConnection`: tai file MP3 tu `audioUrl`.
- Internal app storage: luu file download o `files/audiobooks/*.mp3`.
