# Yapılacaklar: `refactor-project` dalını cihazda deneme (uygulama)

Bu not, bu daldaki işi yerelde derleyip cihazda deneyecek kişi ya da Claude için yazıldı. Adımları sırayla uygula; bir adım beklenmedik sonuç verirse dur ve kullanıcıya sor.

**Bu dal yalnızca yeni sunucuyla çalışır.** Önce `hemen-lazim-spring` reposunun aynı adlı dalındaki `YAPILACAKLAR.md`'yi uygula: veritabanı göçünü yap ve yeni sunucuyu başlat. Eski sunucuyla talep açma, süre uzatma ve hata metinleri çalışmaz.

## Bu dalda ne var
`main`'e göre farklar: `git log --oneline origin/main..refactor-project`. Özetle:
- **Görünüm:** turuncu tema (koyu tema dahil), "Hemen Lazım" adı ve ikonu, Android 12 açılış ekranı.
- **Gezinme:** 4 sekme (Keşfet · İlanlarım · Sohbetler · Profil) ve "+ Talep aç". Keşfet'te harita altında talep listesi, detay alt sayfada.
- **Talepler:** tek sayfalık talep formu; İlanlarım'da tamamla, iptal ve süre uzat.
- **Bildirim ve oturum:**
  - Bildirime dokununca sohbet açılıyor, uygulama kapalıyken de.
  - Oturum düşünce mesajla Giriş'e dönülüyor.
  - Çıkışta soket, FCM kaydı ve refresh token temizleniyor.
- **İki dil:**
  - İngilizce varsayılan (`res/values/`), Türkçe `res/values-tr/`. Desteklenmeyen dildeki telefon İngilizce görür.
  - Sunucu hatası koda göre uygulamanın kendi metniyle gösteriliyor.
  - Android 13+'ta Profil → Dil, uygulamanın dilini telefondan ayrı değiştiriyor.

## Ortam
- **Android Studio** (JDK 17 ya da üstü).
- **Git'e girmeyen dosyalar** yerinde olmalı: `app/google-services.json` (Firebase) ve `app/src/main/res/values/google_maps_api.xml` (`google_maps_key`).
- **Sunucu adresi** `network/RetrofitClient.kt`'de: `BASE_URL` ve `WS_URL`, `10.0.2.2:8080`. Bu adres emülatörden bilgisayara gider.
  - Gerçek telefonda bilgisayarın yerel IP'sini yaz ve `res/xml/network_security_config.xml`'e ekle.
  - Bu yerel değişikliği commit'leme.
- **Push:** Google Play'li emülatör ya da gerçek cihaz gerekir.

## Adımlar
1. **Dalı al:** `git fetch origin` ve `git checkout refactor-project`.
2. **Derle ve test et:** `./gradlew :app:assembleDebug :app:testDebugUnitTest` (Windows: `gradlew.bat`). 20 birim testi geçmeli.
   - **Dikkat:** bulut ortamında jitpack'e erişilemediği için bu dal STOMP kütüphanesi olmadan derlendi; `WebSocketManager.kt`'nin yerine bir taslak kullanıldı.
   - Gerçek `WebSocketManager.kt` ilk kez burada derlenecek. Dosyadaki tek değişiklik `connectedUserId`: hesap değişince soket yeniden kuruluyor.
   - Derleme hatası çıkarsa düzelt.
3. **Kur:** `./gradlew :app:installDebug`.
4. **Cihazda dene** (yeni sunucu çalışırken; teklif ve sohbet için iki hesap ve iki cihaz ya da emülatör gerekir):
   - **Gezinme:**
     - Keşfet'te geri tuşu uygulamadan çıkıyor mu?
     - Sekmeler arası geçişte liste kaydırması ve harita durumu korunuyor mu?
   - **Talep aç:** yayınla → İlanlarım'a dönüş ve "Talebin yayında" mesajı. İlk yayında "Teklif gelince haber verelim mi?" sorusu, ardından Android 13+ bildirim izni.
   - **İlanlarım:** tamamla, onaylı iptal, süre uzat (en fazla şimdiden 24 saat). Kalan süre dakikada bir güncelleniyor mu?
   - **Teklif:**
     - Diğer hesapla talepte "Bende var, verebilirim" → talep sahibine push gidiyor, sohbet açılıyor.
     - İkinci denemede "Talep sahibine zaten haber vermiştin" çıkmalı.
   - **Sohbet:** mesajlar karşı tarafa anında geliyor mu? Okunmamış rozetleri doğru mu?
   - **Push'a dokunma:** sohbet açılıyor mu; uygulama kapalıyken de, açıkken de.
   - **Oturum düşmesi:**
     - Sunucuyu geçici olarak `--jwt.access.minutes=1` argümanıyla başlat, giriş yap.
     - `refresh_tokens` tablosunda o kullanıcının satırını sil.
     - Bir dakika sonra uygulamada bir işlem yap → "Oturumun sona erdi, lütfen tekrar giriş yap." mesajıyla Giriş ekranı gelmeli.
     - Sonra sunucuyu argümansız yeniden başlat.
   - **Koyu tema:** her ekran okunuyor mu?
   - **Dil:**
     - Telefonu İngilizce ve Türkçe yap.
     - Android 13+'ta Profil → Dil'den yalnızca uygulamanın dilini değiştir.
     - Saat 12/24 saat, tarih, "1,5 km / 1.5 km", "2 Paket / 2 packs" dile uyuyor mu?
   - **Hata metinleri** iki dilde de anlaşılır mı:
     - yanlış şifre;
     - alınmış kullanıcı adı ya da e-posta (alanın altında);
     - kapanmış talebe teklif;
     - sunucu kapalıyken ağ hatası.
   - **Push iki dilde:** uygulama öndeyken de, arkadayken de metin telefonun (ya da seçilen uygulama dilinin) dilinde gelmeli.
5. **Hatalar:**
   - Bulduğun hataları düzelt ve her düzeltmeyi ayrı commit olarak `refactor-project`'e at.
   - Düzeltemediklerini ya da karar gerektirenleri kullanıcıya listele.
6. **Bitince:**
   - Sonucu kullanıcıya özetle.
   - Her şey yolundaysa iki repoda da `refactor-project`'i `main`'e birleştirmeyi öner. Kullanıcı onaylamadan birleştirme ve `main`'e push'lama.

## Kurallar (bu dalda yapılan değişikliklerle gelen)
- **Metinler:** yeni metin iki dosyaya da girer: `res/values/strings.xml` (İngilizce) ve `res/values-tr/strings.xml` (Türkçe).
  - Koda metin gömülmez. ViewModel'ler mesajı `@StringRes Int` olarak tutar.
  - Sayı ve tarih için `ui/TimeTexts.kt`'deki işlevler kullanılır.
- **Hata kodları:** sunucunun kodlarıyla `network/ApiErrors.kt` (`ApiCodes`) aynı olmalı; her kodun metni `ui/ErrorMessages.kt`'de.
- **Push sözleşmesi:**
  - `notif_supply_offer_title` ve `notif_supply_offer_body` adları sunucuyla sözleşme; değiştirme.
  - Yeni bir push anahtarı eklenirse üç yere girer:
    - `firebase/HemenLazimFirebaseMessagingService.localized()`;
    - iki `strings.xml`;
    - `res/raw/keep.xml`.
- **Önizlemeler:** `uix/preview/PreviewSupport.kt`'deki `@ScreenPreviews` ve `@ComponentPreviews` Türkçe açık, Türkçe koyu ve İngilizce çiziyor. Yeni ekranlarda da bunları kullan.
- **Gizli dosyalar** commit'lenmez.
- **Commit mesajları** İngilizce, kısa ve ne yaptığını söyleyen cümle; kod yorumları Türkçe.

## Sonra (ayrı işler, kullanıcı onayıyla)
- **Arkadayken gelen push:**
  - Şu an FCM'in "Miscellaneous" kanalına düşüyor ve uygulama ikonuyla görünüyor.
  - Manifest'e şu meta-data'lar eklenmeli:
    - `com.google.firebase.messaging.default_notification_channel_id` (kanal `hemen_lazim_notifications`);
    - `default_notification_icon` ve `default_notification_color`.
  - Kanal açılışta oluşturulmalı.
- **Release derleme:**
  - R8 açılınca Gson DTO'ları, `ErrorBody`, Retrofit, Stomp ve RxJava için keep kuralları.
  - dev/prod flavor ve `BuildConfig`'ten sunucu adresi.
  - HTTPS/WSS, imzalama ve AAB.
- **Güvenlik:** token'lar şifreli depoda olmalı; `allowBackup` kapsamı daraltılmalı.
- **Hesap silme arayüzü:** sunucuda `DELETE /api/users/me` var.
- **Ürün:** teklif kabul ve ret, ilana bağlı sohbet, puanlama.
