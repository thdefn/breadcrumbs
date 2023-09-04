package wanted.breadcrumbs.page;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@RequiredArgsConstructor
@Service
public class PageService {
    private final PageRepository pageRepository;

    public void create(long parentId, PageForm form) {
        Long pageId = pageRepository.save(PageVo.builder()
                .title(form.getTitle())
                .content(form.getContent())
                .parentPageId(parentId).build());

        PageCache.addPageBreadCrumb(pageId, parentId);
    }

    public PageDto read(Long pageId) {
        PageVo vo = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("no such data exists"));

        return PageDto.builder()
                .id(vo.getId())
                .title(vo.getTitle())
                .content(vo.getContent())
                .subPages(vo.getSubPages())
                .parentPageId(vo.getParentPageId())
                .breadcrumbs(PageCache.breadcrumb.get(pageId))
                .build();
    }

}
