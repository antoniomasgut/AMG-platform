package com.amg.digitalitzacio.widget.api;

import com.amg.digitalitzacio.widget.application.WidgetChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/widget")
@RequiredArgsConstructor
public class WidgetController {

    private final WidgetChatService widgetChatService;

    @Value("${app.api.base-url:https://api.amgdl.com}")
    private String apiBaseUrl;

    @GetMapping("/{siteId}/config")
    public ResponseEntity<WidgetChatService.WidgetConfig> getConfig(@PathVariable UUID siteId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .body(widgetChatService.getConfig(siteId));
    }

    @GetMapping(value = "/{siteId}/loader")
    public ResponseEntity<String> serveJs(@PathVariable String siteId) {
        String js = buildWidgetJs(siteId, apiBaseUrl);
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=300")
                .body(js);
    }

    @PostMapping("/{siteId}/sessions")
    public ResponseEntity<WidgetChatService.CreateSessionResult> createSession(
            @PathVariable UUID siteId,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "unknown") String ip) {
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .body(widgetChatService.createSession(siteId, ip.split(",")[0].strip()));
    }

    @PostMapping(value = "/{siteId}/contact",
                 consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> submitContact(
            @PathVariable UUID siteId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "message", required = false) String message) {

        var result = widgetChatService.submitContactForm(siteId, name, email, phone, message);
        if (result.redirectUrl() != null && !result.redirectUrl().isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                    .header(HttpHeaders.LOCATION, result.redirectUrl())
                    .build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .body(result);
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<WidgetChatService.SendMessageResult> sendMessage(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "unknown") String ip) {
        return ResponseEntity.ok()
                .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .body(widgetChatService.sendMessage(sessionId, body.get("message"), ip.split(",")[0].strip()));
    }

    private String buildWidgetJs(String siteId, String apiBase) {
        return """
(function() {
  'use strict';
  var SITE_ID = '%s';
  var API = '%s';
  // Interceptor de formularis de contacte — s'executa sempre, independentment del chat/WA
  function amgIntercept() {
    document.querySelectorAll('form').forEach(function(f) {
      var act = (f.getAttribute('action') || '').toLowerCase();
      if (act.indexOf('/api/v1/') !== -1) return;  // ja és un endpoint AMG
      if (act.indexOf('search') !== -1) return;      // formulari de cerca
      if (f.querySelector('[type="password"]')) return; // login/registre
      if (f.dataset.amgDone) return;
      f.dataset.amgDone = '1';
      f.addEventListener('submit', function(e) {
        e.preventDefault();
        var q = function(s) { return f.querySelector(s); };
        var nf = q('[name="name"],[name="nom"],[name="nombre"],[name="full_name"],[name="firstname"],[name="first_name"]') || q('[type="text"]');
        var ef = q('[type="email"],[name="email"],[name="correu"],[name="correo"]');
        var pf = q('[type="tel"],[name="phone"],[name="tel"],[name="telefon"],[name="telefono"],[name="mobile"],[name="movil"]');
        var mf = q('textarea') || q('[name="message"],[name="missatge"],[name="mensaje"],[name="msg"],[name="comment"]');
        var body = new URLSearchParams({
          name:    nf ? nf.value.trim() : '',
          email:   ef ? ef.value.trim() : '',
          phone:   pf ? pf.value.trim() : '',
          message: mf ? mf.value.trim() : ''
        }).toString();
        fetch(API + '/api/v1/widget/' + SITE_ID + '/contact', {
          method: 'POST',
          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
          body: body
        }).then(function() {
          var ok = document.createElement('div');
          ok.setAttribute('style', 'padding:14px 18px;background:#f0fdf4;border:1px solid #86efac;border-radius:8px;color:#166534;font-family:sans-serif;font-size:14px;margin-top:8px');
          ok.textContent = 'Missatge enviat. Ens posarem en contacte aviat!';
          f.style.display = 'none';
          if (f.parentNode) f.parentNode.insertBefore(ok, f.nextSibling);
        }).catch(function() {
          delete f.dataset.amgDone;
          f.submit();
        });
      }, { once: true });
    });
  }
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', amgIntercept);
  } else {
    amgIntercept();
  }
  fetch(API + '/api/v1/widget/' + SITE_ID + '/config')
    .then(function(r) { return r.json(); })
    .then(function(cfg) {
      if (!cfg.chatEnabled && !cfg.waNumber) return;
      var style = document.createElement('style');
      style.textContent = '.amg-w{position:fixed;bottom:20px;right:20px;z-index:9999;display:flex;flex-direction:column;align-items:flex-end;gap:10px}.amg-b{width:52px;height:52px;border-radius:50%%;border:none;cursor:pointer;display:flex;align-items:center;justify-content:center;box-shadow:0 4px 12px rgba(0,0,0,.2);transition:transform .2s}.amg-b:hover{transform:scale(1.08)}.amg-wa{background:#25d366}.amg-ch{background:#4f46e5}.amg-cw{position:fixed;bottom:90px;right:20px;width:320px;max-height:480px;background:#fff;border-radius:16px;box-shadow:0 8px 32px rgba(0,0,0,.15);display:flex;flex-direction:column;overflow:hidden;z-index:9999}.amg-hd{background:#4f46e5;color:#fff;padding:14px 16px;font-weight:600;font-size:14px;display:flex;justify-content:space-between;align-items:center}.amg-bd{flex:1;overflow-y:auto;padding:12px;display:flex;flex-direction:column;gap:8px;min-height:200px}.amg-m{max-width:80%%;padding:8px 12px;border-radius:12px;font-size:13px;line-height:1.4;font-family:sans-serif;word-break:break-word}.amg-mu{background:#4f46e5;color:#fff;align-self:flex-end;border-bottom-right-radius:4px}.amg-mb{background:#f0f0f0;color:#333;align-self:flex-start;border-bottom-left-radius:4px}.amg-in{display:flex;padding:8px;border-top:1px solid #eee;gap:6px}.amg-in input{flex:1;border:1px solid #ddd;border-radius:20px;padding:7px 12px;font-size:13px;outline:none;font-family:sans-serif}.amg-in input:focus{border-color:#4f46e5}.amg-in button{background:#4f46e5;color:#fff;border:none;border-radius:50%%;width:32px;height:32px;cursor:pointer;font-size:18px}.amg-hide{display:none!important}';
      document.head.appendChild(style);
      var wrap = document.createElement('div'); wrap.className = 'amg-w';
      var sessId = null, chatWin = null;
      if (cfg.waNumber) {
        var wb = document.createElement('button'); wb.className = 'amg-b amg-wa'; wb.title = 'WhatsApp';
        wb.innerHTML = '<svg width="26" height="26" viewBox="0 0 24 24" fill="#fff"><path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/></svg>';
        wb.onclick = function() { var n = cfg.waNumber.replace(/\\D/g,''); window.open('https://wa.me/'+n,'_blank'); };
        wrap.appendChild(wb);
      }
      if (cfg.chatEnabled) {
        chatWin = document.createElement('div'); chatWin.className = 'amg-cw amg-hide';
        chatWin.innerHTML = '<div class="amg-hd"><span>'+(cfg.businessName||'Xat')+'</span><button id="amg-close" style="background:transparent;border:none;color:#fff;font-size:20px;cursor:pointer;line-height:1">×</button></div><div class="amg-bd" id="amg-bd"></div><div class="amg-in"><input type="text" id="amg-inp" placeholder="Escriu un missatge..." /><button id="amg-send">›</button></div>';
        document.body.appendChild(chatWin);
        var cb = document.createElement('button'); cb.className = 'amg-b amg-ch'; cb.title = 'Xat';
        cb.innerHTML = '<svg width="24" height="24" viewBox="0 0 24 24" fill="#fff"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-2 12H6v-2h12v2zm0-3H6V9h12v2zm0-3H6V6h12v2z"/></svg>';
        function openChat()  { chatWin.classList.remove('amg-hide'); wrap.classList.add('amg-hide'); if (!sessId) startSess(); }
        function closeChat() { chatWin.classList.add('amg-hide'); wrap.classList.remove('amg-hide'); }
        cb.onclick = openChat;
        chatWin.querySelector('#amg-close').onclick = closeChat;
        wrap.appendChild(cb);
        chatWin.querySelector('#amg-inp').addEventListener('keydown', function(e) { if (e.key==='Enter') send(); });
        chatWin.querySelector('#amg-send').onclick = send;
      }
      document.body.appendChild(wrap);
      function addMsg(role, text) {
        var bd = chatWin.querySelector('#amg-bd');
        var d = document.createElement('div'); d.className = 'amg-m '+(role==='user'?'amg-mu':'amg-mb'); d.textContent = text;
        bd.appendChild(d); bd.scrollTop = bd.scrollHeight;
      }
      function startSess() {
        fetch(API+'/api/v1/widget/'+SITE_ID+'/sessions',{method:'POST'})
          .then(function(r){return r.json();}).then(function(d){sessId=d.sessionId;addMsg('bot',d.greeting);})
          .catch(function(){addMsg('bot','El xat no està disponible ara.');});
      }
      function send() {
        var inp = chatWin.querySelector('#amg-inp'); var txt = inp.value.trim();
        if (!txt||!sessId) return; inp.value=''; addMsg('user',txt);
        fetch(API+'/api/v1/widget/sessions/'+sessId+'/messages',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({message:txt})})
          .then(function(r){return r.json();}).then(function(d){addMsg('bot',d.reply);if(d.terminated)sessId=null;})
          .catch(function(){addMsg('bot','Error de connexió.');});
      }
    }).catch(function(){});
})();
""".formatted(siteId, apiBase);
    }
}
