package wanted.breadcrumbs.page;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class PageCache {
    private final PageRepository pageRepository;
    public static Map<Long, ArrayList<String>> breadcrumb = new HashMap<>();

    @PostConstruct
    private void getBreadcrumb() {
        pageRepository.findByOrderByParentPageId()
                .forEach(page -> addPageBreadCrumb(page.getId(), page.getParentPageId()));

        for (Map.Entry<Long, ArrayList<String>> entry : breadcrumb.entrySet())
            log.error(entry.getKey() + ":" + entry.getValue().toString());
    }

    public static void addPageBreadCrumb(Long pageId, Long parentPageId) {
        if (pageId == null) return;
        ArrayList<String> list = new ArrayList<>(breadcrumb.getOrDefault(parentPageId, new ArrayList<>()));
        list.add("페이지 " + pageId);
        breadcrumb.put(pageId, list);

    }
}
