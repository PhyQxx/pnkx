package com.pnkx.web.service.intent;

import com.pnkx.life.service.webarticle.WebArticleUtils;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import java.net.InetAddress;
import java.net.URI;
import java.io.IOException;

@Component
public class RecipePageReader {
    public String read(String url) throws IOException {
        for (int redirects = 0; redirects < 4; redirects++) {
            URI uri = URI.create(url);
            if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
                    || uri.getUserInfo() != null || WebArticleUtils.isInternalUrl(url)) throw new IOException("不支持的地址");
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()
                        || (address.getAddress().length == 16 && (address.getAddress()[0] & 0xfe) == 0xfc))
                    throw new IOException("不支持的地址");
            }
            var response = Jsoup.connect(url).userAgent("Mozilla/5.0")
                    .timeout(15000).maxBodySize(1000000).followRedirects(false).execute();
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                String location = response.header("Location");
                if (location == null) throw new IOException("无效跳转");
                url = uri.resolve(location).toString();
                continue;
            }
            var doc = response.parse();
            String structured = doc.select("script[type=application/ld+json]").stream()
                    .map(e -> e.data()).collect(java.util.stream.Collectors.joining("\n"));
            doc.select("script,style,nav,footer").remove();
            String content = structured + "\n" + doc.text();
            if (content.isBlank()) throw new IOException("正文为空");
            return content.substring(0, Math.min(content.length(), 30000));
        }
        throw new IOException("跳转过多");
    }
}
