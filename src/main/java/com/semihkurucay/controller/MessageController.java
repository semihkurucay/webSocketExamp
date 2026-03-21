package com.semihkurucay.controller;

import com.semihkurucay.entity.MessageChat;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

/**
 * GENEL SOHBET (BROADCAST / YAYIN) KONTROLCÜSÜ
 * Bu sınıf, Point-to-Point (birebir) mesajlaşmanın aksine, "Megafon" mantığıyla çalışır.
 * Bir kişiden gelen mesajı alır ve o an o odayı (kanalı) dinleyen HERKESE aynı anda iletir.
 * Genellikle "Genel Sohbet Odaları", "Duyuru Sistemleri" veya "Borsa Canlı Veri Akışları" için kullanılır.
 */
@RestController // Bu sınıfın bir Controller olduğunu ve REST/STOMP isteklerini karşılayabileceğini belirtir.
public class MessageController {

    /**
     * MESAJ YAKALAMA VE HERKESE YAYINLAMA (BROADCASTING)
     * * İşleyiş: İstemciden gelen mesajı yakalar, (istenirse) işler ve belirtilen ortak kanala fırlatır.
     * * @param message Frontend'den JSON olarak gelen ve Spring tarafından MessageChat objesine dönüştürülen veri.
     * @return Döndürülen nesne (message), @SendTo anotasyonu sayesinde otomatik olarak JSON'a çevrilip kanala basılır.
     */

    // GİRİŞ KAPISI: Frontend'den "stompClient.send('/app/chat')" koduyla tetiklenir.
    // (WebSocketConfig içindeki "/app" ön ekinden dolayı doğrudan buraya düşer).
    @MessageMapping("/chat")

    // ÇIKIŞ KAPISI (MEGAFON): Metot işini bitirip 'return' ettiği anda, o veriyi alıp bu adrese fırlatır.
    // Frontend'de "stompClient.subscribe('/topic/public')" diyerek bu kanalı dinleyen İSTİSNASIZ HERKES bu mesajı anında görür.
    // Not: Yorum satırında "/topic/reply" yazmışsın ancak koda "/topic/public" yazmışsın.
    // Sistem her zaman anotasyonun içindeki adresi ("/topic/public") baz alır.
    @SendTo("/topic/public")
    private MessageChat sendMessage(MessageChat message){

        // İŞ MANTIĞI (BUSINESS LOGIC) KATI
        // Şu an hiçbir kontrol yok, gelen mesajı doğrudan return ederek kanala basıyoruz.
        // Ancak gerçek bir projede yayına çıkmadan önce buraya şunlar eklenebilir:
        // 1. message.setTimestamp(new Date()); -> Mesaja sunucu saati eklenebilir.
        // 2. Küfür/Hakaret filtresi eklenebilir (Eğer mesajda küfür varsa return edilmez, böylece kimseye gitmez).
        // 3. messageRepository.save(message); -> Mesaj kalıcı olması için veritabanına kaydedilebilir.

        return message; // Return edilen bu değer, @SendTo adresine doğru yola çıkar.
    }
}