### 목표

노션과 유사한 간단한 페이지 관리 API 를 구현하자. 
각 페이지는 제목, 컨텐츠, 그리고 서브 페이지를 가질 수 있다. 
또한, 특정 페이지에 대한 브로드 크럼스(Breadcrumbs) 정보를 반환해야 한다.

### 요구사항
**페이지 정보 조회 API**: 특정 페이지의 정보를 조회할 수 있는 API를 구현하세요.

- 입력: 페이지 ID
- 출력: 페이지 제목, 컨텐츠, 서브 페이지 리스트, **브로드 크럼스 ( 페이지 1 > 페이지 3 > 페이지 5)**
- 컨텐츠 내에서 서브페이지 위치 고려  X



### 테이블 구조

| 필드      | 타입             | 제약 조건  |
|---------|----------------|--------|
| id      | `big int`      | PK     |
| title   | `varchar(100)` |   |
| content | `blob`         |  |
| parent_page_id | `big int`      | FK     |

- 상위 페이지와 하위 페이지 간 **1:N 관계**
  - 상위 페이지가 여러개의 하위 페이지를 가지고 있을 수 있음

<img width="212" alt="스크린샷 2023-09-04 오후 7 37 24" src="https://github.com/thdefn/breadcrumbs/assets/80521474/a0339dca-de44-4c36-bbfc-adb30efa291a">


### 비즈니스 로직
- **스프링부트가 올라갈 때** - `HashMap` 을 통한 브로드 크럼스 정보 캐싱
  
  <img width="160" alt="스크린샷 2023-09-04 오전 8 09 19" src="https://github.com/thdefn/breadcrumbs/assets/80521474/fd20615a-59dc-4d65-9966-9d96f49f8a2a">

  ```sql
    select parent_page_id, id  from page order by parent_page_id
  ```
  - **하위 페이지 브로드 크럼스 = 상위 페이지 브로드 크럼스 + 하위 페이지 아이디**
    - 상위 페이지부터 캐싱 진행하기 위해 `parent_page_id` 오름차순 조건을 추가함
  ```
    public static Map<Long, ArrayList<String>> breadcrumb = new HashMap<>();
  
    @PostConstruct
    private void getBreadcrumb() {
        pageRepository.findByOrderByParentPageId()
                .forEach(page -> addPageBreadCrumb(page.getId(), page.getParentPageId()));
    }

    public static void addPageBreadCrumb(Long pageId, Long parentPageId) {
        if (pageId == null) return;
        ArrayList<String> list = new ArrayList<>(breadcrumb.getOrDefault(parentPageId, new ArrayList<>()));
        list.add("페이지 " + pageId);
        breadcrumb.put(pageId, list);
    }
  ```
  - <페이지 아이디, 브로드 크럼스 list> 로 이루어진 `Hash Map` 이다.
  - 상위 페이지부터 캐싱을 진행함
  - 하위 페이지는 상위페이지에 저장된 `ArrayList` 에 하위 페이지 아이디를 추가하는 방식
  - `@PostConstruct` 를 통해 Bean 이 생성될 때만 캐싱이 진행될 수 있게 함
---
- **페이지가 생성될 때** - `HashMap`에 생성되는 페이지의 브로드 크럼스 업데이트
  ```
    public void create(long parentId, PageForm form) {
        Long pageId = pageRepository.save(PageVo.builder()
                .title(form.getTitle())
                .content(form.getContent())
                .parentPageId(parentId).build());

        PageCache.addPageBreadCrumb(pageId, parentId);
    }
  ```
---
- 페이지를 조회할 때 - `HashMap`에서 브로드 크럼스 조회
   ```sql
  select p1.id, p1.title, p1.content, p1.parent_page_id, p2.id
  from page p1
  left join page p2 on p2.parent_page_id = p1.id
  where p1.id = ?;
   ```
  - 서브 페이지는 DB 에서 조회
    - DB 조회시 `left join` 해 정보를 가져옴
    - 현재 페이지를 부모 페이지로 갖는 페이지 아이디를 받아옴
  ```
    public PageDto read(Long pageId) {
        PageVo vo = pageRepository.findById(pageId)
                .orElseThrow(() -> new RuntimeException("no such data exists"));

        return PageDto.builder()
                .id(vo.getId())
                .title(vo.getTitle())
                .content(vo.getContent())
                .subPages(vo.getSubPages())
                .breadcrumbs(PageCache.breadcrumb.get(pageId))
                .build();
    }
  ```
  
### 실행 예시

- 상위 페이지 조회 시
  - `GET localhost:8080/pages/2`
  ```
  {
    "id": 2,
    "title": "하이하이",
    "content": "하이하이",
    "parentPageId": 0,
    "subPages": [
        3,
        4,
        9,
        10
    ],
    "breadcrumbs": [
        "페이지 2"
    ]
  }
  ```

- 하위 페이지 조회 시
  - `GET localhost:8080/pages/10`
  ```
  {
    "id": 10,
    "title": "금산",
    "content": "금산사",
    "parentPageId": 2,
    "subPages": [],
    "breadcrumbs": [
        "페이지 2",
        "페이지 10"
    ]
  }
  ```

### 현재 구조의 장단점
- 장점
  1. 조회 성능에 강하다
     - 페이지 정보를 조회할 때 기저장된 브로드 크럼스 캐싱 정보를 가져오면 됨
  2. page 삭제 시 DB와 싱크를 맞추기 쉽다.
     - `subPages` 컬럼은 따로 캐싱하면 하위 페이지 삭제시 DB와 싱크를 맞추기 어렵다.
     - 캐싱된 브로드 크럼스 데이터는 삭제 시 O(1)이다.
     - 브로드 크럼스 데이터는 따로 삭제하지 않아도 DB 에서 삭제되면 조회되지 않고, 다른 데이터에도 영향을 미치지 않는다.
- 단점
  1. Deep Copy 를 이용하기 때문에, 스프링부트를 띄울 때 브로드 크럼스를 캐싱하는 로직이 O(N^2)이다.
