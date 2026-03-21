package com.semihkurucay.controller;

import com.semihkurucay.entity.PrivateMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * MÜŞTERİ SERVİSİ VE YÖNLENDİRME KONTROLCÜSÜ (ANA BEYİN)
 * Bu sınıf, sistemin iş mantığını barındırır. Gelen mesajları karşılar, kimlik ataması yapar
 * ve mesajları doğru kişinin özel frekansına (kanalına) yönlendirir.
 * Hem klasik HTTP (REST) hem de TCP (WebSocket) bağlantılarını aynı anda yönetir.
 */
@RequiredArgsConstructor // Lombok: 'final' olarak tanımlanmış değişkenler için otomatik Constructor (Dependency Injection) oluşturur.
@RestController // Bu sınıfın bir Spring MVC Controller olduğunu ve metotların varsayılan olarak JSON (Response Body) döneceğini belirtir.
public class CustomerServiceController {

    /**
     * DİNAMİK MESAJ YÖNLENDİRİCİ (SimpMessagingTemplate)
     * Spring'in bize sunduğu en güçlü WebSocket yönlendirme aracıdır.
     * @SendTo anotasyonunun aksine, mesajı statik (sabit) bir yere değil, çalışma zamanında (runtime)
     * kod içinden belirlediğimiz dinamik bir adrese (kullanıcıya özel kanala) göndermemizi sağlar.
     */
    private final SimpMessagingTemplate simpMessagingTemplate;

    /**
     * 1. KİMLİK ATAMA (REST API UÇ NOKTASI)
     * * İşleyiş: Sisteme üye olmadan giriş yapan "Ziyaretçi" müşterilere geçici bir kimlik (ID) vermek için kullanılır.
     * Frontend HTTP İsteği: GET http://localhost:8080/api/chat/get-id
     * * @return Rastgele üretilmiş eşsiz bir UUID string'i (Örn: "550e8400-e29b-41d4-a716-446655440000")
     */
    @GetMapping("/api/chat/get-id")
    public String generateCustomerChatId(){
        // Not: İleride bu aşamada veritabanına "yeni bir sohbet oturumu başladı" kaydı atılabilir.
        return UUID.randomUUID().toString();
    }

    /**
     * 2. BİREBİR MESAJ YAKALAMA VE YÖNLENDİRME (WEBSOCKET UÇ NOKTASI)
     * * İşleyiş: Frontend'den gelen STOMP mesajlarını yakalar ve sadece ilgili alıcıya iletir.
     * Geliş Yolu: Frontend'den "stompClient.send('/app/private-chat')" koduyla tetiklenir.
     * (Not: WebSocketConfig sınıfındaki "/app" ön ekinden dolayı buraya yönlenir).
     * * @param message Frontend'den gelen JSON paketinin Spring tarafından otomatik dönüştürülmüş (Deserialized) Java Objesi.
     */
    @MessageMapping("/private-chat")
    public void processPrivateMessage(PrivateMessage message){

        // KONTROL VE LOGLAMA
        // Sunucu konsoluna mesajın kimden kime gittiğini yazdırıyoruz.
        // Mimari Not: Veritabanına mesajı kaydetme (repository.save(message)) veya kelime filtresi gibi iş kuralları tam bu satırda uygulanmalıdır.
        // Güvenlik Notu: İleride JWT (Spring Security) entegre edildiğinde, güvenliği sağlamak için 'message.getFrom()' yerine
        // doğrudan token üzerinden gelen 'principal.getName()' kullanılmalıdır!
        System.out.println("İçerik: " + message.getMessage() + " -> Kimden: " + message.getFrom() + " -> Kime: " + message.getTo());

        // HEDEFE FISILDAMA (POINT-TO-POINT ROUTING)
        // Kullanımı: simpMessagingTemplate.convertAndSend(GidecekAdres, GidecekVeri)
        //
        // Dinamik Adresleme: "/topic/messages/" + HedefKişininIDsi
        // Örnek 1 (Müşteriden Temsilciye Giden Mesaj): /topic/messages/customer-service
        // Örnek 2 (Temsilciden Müşteriye Dönen Mesaj): /topic/messages/fdcbf2da-8a00-4360-880b-ffa658fd7ed3
        //
        // Sonuç: Frontend'de sadece bu özel adrese abone (subscribe) olmuş olan istemci bu mesajı görebilir.
        // Diğer bağlantılar bu veriden tamamen habersizdir.
        simpMessagingTemplate.convertAndSend("/topic/messages/" + message.getTo(), message);
    }

    /**
     * MERKEZ YÖNLENDİRİCİ (ROUTER) VE GÜVENLİK KONTROLÜ
     * Sistemdeki tüm anlık mesajlaşma trafiğini karşılayan, gönderenin kimliğini denetleyen
     * ve mesajın kime gideceğine (Ortak Havuz mu, Bireysel Kanal mı) karar veren ana iş motorudur.
     * * @param message Frontend'den JSON olarak gelen ve Java objesine dönüştürülen mesaj paketi (from, to, message).
     * @param principal Spring Security'nin enjekte ettiği nesne. Oturum açmış Temsilci ise dolu, anonim Müşteri ise 'null' gelir.
     */
    @MessageMapping("/private-chat") // Frontend'den "stompClient.send('/app/private-chat')" koduyla burası tetiklenir.
    public void processPrivateMessage(PrivateMessage message, Principal principal) {

        // =========================================================================
        // 1. KİMLİK GÜVENLİĞİ (SPOOFING ENGELLEME)
        // Frontend'den (Javascript) gelen "message.getFrom()" değerine asla körü körüne güvenilmez.
        // Kötü niyetli biri JS üzerinden kimliğini değiştirip başkası adına mesaj atabilir.
        // =========================================================================

        if (principal != null) {
            // Eğer principal doluysa, bu kişi JWT Token ile giriş yapmış yetkili bir TEMSİLCİDİR.
            // Frontend'in gönderdiği sahte olabilecek 'from' değerini eziyor ve
            // doğrudan güvenilir Token'dan aldığımız gerçek kimliği mesaja zorla basıyoruz.
            message.setFrom(principal.getName());
        }
        // Not: Eğer principal null ise (Ziyaretçi/Müşteri), sisteme giriş yapmadıkları için mecburen
        // frontend'in gönderdiği (bizim REST API'den aldığı) UUID'yi kullanmalarına izin veririz.


        // =========================================================================
        // 2. YÖNLENDİRME (ROUTING) MAKASI
        // Mesajın hedefi (to) neresi? Bir gruba mı (Broadcast) yoksa tek kişiye mi (Point-to-Point) gidecek?
        // =========================================================================

        if ("customer-service".equals(message.getTo())) {

            // SENARYO A: MÜŞTERİDEN -> TEMSİLCİ HAVUZUNA (YARDIM ÇAĞRISI)
            // Müşteri sisteme girdiğinde kime yazacağını bilemez, bu yüzden hedefi "customer-service" olarak yollar.
            // Bu mesajı alıp tek bir kişiye değil, JWT ile bağlanan TÜM temsilcilerin dinlediği "Ortak Destek Havuzu"na fırlatıyoruz.

            simpMessagingTemplate.convertAndSend("/topic/support-pool", message);

            // Konsola bilgi basıyoruz (İleride buraya db.save(message) gibi veritabanı kayıt kodları eklenebilir)
            System.out.println("Havuza Mesaj Düştü: " + message.getFrom() + " -> " + message.getMessage());

        } else {

            // SENARYO B: TEMSİLCİDEN -> MÜŞTERİYE (NOKTA ATIŞI CEVAP)
            // Temsilci, havuzdaki bir mesaja cevap yazdığında, hedef (to) kısmına müşterinin o eşsiz UUID'sini yazar.
            // Backend bu mesajı havuza atmaz. SADECE o UUID'ye sahip müşterinin dinlediği kişisel frekansa yönlendirir.

            simpMessagingTemplate.convertAndSend("/topic/messages/" + message.getTo(), message);

            System.out.println("Müşteriye Cevap Gitti: " + message.getFrom() + " -> " + message.getTo());
        }
    }
}

/**
 * MİMARİ NOT: TEK METOTLA TAM KONTROL (RECEIVE & SEND)
 * * Soru: Tek bir metot yazarak hem mesaj alma hem de mesaj atma işini mi hallediyorum?
 * Cevap: KESİNLİKLE EVET. Spring Boot'ta bu döngü tek bir metot içinde tamamlanır.
 * * 1. ALMA (RECEIVE) İŞLEMİ:
 * Metodun başındaki @MessageMapping("/private-chat") anotasyonu senin "Gelen Kutun"dur.
 * Frontend'den fırlatılan mesaj JSON'dan Java objesine çevrilir ve doğrudan bu metodun
 * 'message' parametresinin içine düşer. (Alma işlemi bitti).
 * * 2. İŞLEME (PROCESS) İŞLEMİ:
 * Metodun gövdesinde (if-else blokları, principal kontrolleri) mesajı analiz edersin,
 * kimden geldiğini doğrularsın ve nereye gideceğine (havuza mı yoksa kişiye mi) karar verirsin.
 * * 3. GÖNDERME (SEND) İŞLEMİ:
 * Metodun en altındaki 'simpMessagingTemplate.convertAndSend(...)' satırı senin "Giden Kutun"dur.
 * İşlediğin mesajı alır ve hedefteki kişinin/grubun kanalına fırlatır. (Gönderme işlemi bitti).
 * * ÖZET:
 * Frontend iki ayrı işlem (stompClient.send ve stompClient.subscribe) yapmak zorundadır.
 * Ama Backend (Sen) tek bir metotla gelen topu göğsünde yumuşatır, yönüne karar verir ve pası atarsın!
 **/