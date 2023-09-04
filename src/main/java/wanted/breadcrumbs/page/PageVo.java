package wanted.breadcrumbs.page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PageVo {
    private Long id;
    private String title;
    private String content;
    private Long parentPageId;
    private List<Long> subPages = new ArrayList<>();

    public void setSubPages(List<Long> subPages){
        this.subPages = subPages;
    }
}
