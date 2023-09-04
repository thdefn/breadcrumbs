package wanted.breadcrumbs.page;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/pages")
@RequiredArgsConstructor
public class PageController {
    private final PageService pageService;

    @PostMapping("/{pageId}")
    private ResponseEntity<Void> create(@Valid @RequestBody PageForm form,
                                        @PathVariable Long pageId) {
        pageService.create(pageId, form);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{pageId}")
    private ResponseEntity<PageDto> read(@PathVariable Long pageId) {
        return ResponseEntity.ok(pageService.read(pageId));
    }
}
