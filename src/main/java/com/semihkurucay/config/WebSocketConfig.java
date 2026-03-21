package com.semihkurucay.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WEB SOCKET VE STOMP MESAJLAŞMA YAPILANDIRMA SINIFI (AĞ GEÇİDİ)
 * * Bu sınıf, sistemin "Trafik Polisi"dir. Hangi yolların açık olduğunu, gelen
 * araçların (mesajların) hangi şeritten gidip hangi şeritten döneceğini belirleyen ana konfigürasyon dosyasıdır.
 */
@Configuration // Spring Boot'a bu sınıfın sıradan bir kod olmadığını, proje başlarken okunması gereken bir ayar dosyası olduğunu söyler.
@EnableWebSocketMessageBroker // Projede WebSocket üzerinden mesajlaşmayı aktif eder ve arka planda bir "Broker" (Mesaj Dağıtıcı) kurar.
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 1. MESAJ YÖNLENDİRME KURALLARI (BROKER AYARLARI)
     * Bağlantı kurulduktan SONRA içerideki mesaj trafiğinin hangi ön eklerle (prefix) yönetileceğini belirler.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {

        // ÇIKIŞ ŞERİDİ (Sunucudan -> İstemciye)
        // Frontend kodu "stompClient.subscribe('/topic/...')" diyerek bu kanalları dinlemeye başlar.
        // Java tarafında SimpMessagingTemplate, gelen mesajları bu ön eke sahip kanallara fırlatır.
        config.enableSimpleBroker("/topic");

        // GİRİŞ ŞERİDİ (İstemciden -> Sunucuya)
        // Frontend bir mesaj göndermek istediğinde adresi mutlaka "/app" ile başlatmak zorundadır.
        // Örn: stompClient.send('/app/private-chat').
        // Spring Boot, başında "/app" olan bir istek gördüğünde bunu doğrudan Controller sınıfındaki @MessageMapping anotasyonlarına yönlendirir.
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 2. BAĞLANTI KAPISI (HANDSHAKE / EL SIKIŞMA)
     * Mesajlaşma başlamadan ÖNCE istemcinin sunucuya ilk teması kuracağı ana kapıyı belirler.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        // ANA KAPI: Frontend'in Web Socket protokolünü başlatmak için çalacağı ilk HTTP kapısıdır.
        // Frontend bağlantı kodu: let socket = new SockJS('http://localhost:8080/ws-chat')
        registry.addEndpoint("/ws-chat")

                // GÜVENLİK (CORS): Tarayıcılar güvenlik gereği farklı portlardan gelen istekleri engeller.
                // "*" değeri geliştirme aşamasında her yerden gelen bağlantıyı kabul etmeyi sağlar.
                // DİKKAT: Canlı ortama (Production) çıkarken "*" yerine gerçek frontend domaini (Örn: https://seninsiten.com) yazılmalıdır!
                .setAllowedOriginPatterns("*")

                // YEDEKLEME (FALLBACK): Her tarayıcı veya şirket ağı (firewall) doğrudan WebSocket'e izin vermeyebilir.
                // SockJS, eğer WebSocket kurulamıyorsa arka planda HTTP Long-Polling gibi alternatif
                // yöntemlere geçerek bağlantının her halükarda kopmadan devam etmesini sağlar.
                .withSockJS();
    }
}