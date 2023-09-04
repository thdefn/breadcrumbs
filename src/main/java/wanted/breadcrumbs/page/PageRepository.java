package wanted.breadcrumbs.page;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PageRepository {
    private final DataSource dataSource;

    public Long save(PageVo page) {
        String sql = "insert into page(title, content, parent_page_id) values (?, ?, ?) ";
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Long pageId = null;
        try {
            con = dataSource.getConnection();
            con.setAutoCommit(false);
            statement = con.prepareStatement(sql);
            statement.setString(1, page.getTitle());
            statement.setString(2, page.getContent());
            statement.setLong(3, page.getParentPageId());
            statement.executeUpdate();

            statement = con.prepareStatement("select id from page order by id desc limit 1");
            resultSet = statement.executeQuery();
            if (resultSet.next()) {
                con.commit();
                pageId = resultSet.getLong(1);
            }
        } catch (SQLException e) {
            log.error("db error occurred", e);
            throw new RuntimeException(e);
        } finally {
            close(con, statement, resultSet);
        }
        return pageId;
    }

    public List<PageVo> findByOrderByParentPageId() {
        String sql = "select parent_page_id, id  from page order by parent_page_id ";
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        ArrayList<PageVo> pages = new ArrayList<>();
        try {
            con = dataSource.getConnection();
            statement = con.prepareStatement(sql);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                pages.add(PageVo.builder()
                        .parentPageId(resultSet.getLong(1))
                        .id(resultSet.getLong(2))
                        .build());
            }
        } catch (SQLException e) {
            log.error("db error occurred", e);
            throw new RuntimeException(e);
        } finally {
            close(con, statement, resultSet);
        }
        return pages;
    }

    public Optional<PageVo> findById(Long pageId) {
        String sql = "select p1.id, p1.title, p1.content, p1.parent_page_id, p2.id " +
                "from page p1 " +
                "left join page p2 on p2.parent_page_id = p1.id " +
                "where p1.id = ? ";
        Connection con = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            con = dataSource.getConnection();
            statement = con.prepareStatement(sql);
            statement.setLong(1, pageId);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                PageVo vo = PageVo.builder()
                        .id(resultSet.getLong(1))
                        .title(resultSet.getString(2))
                        .content(resultSet.getString(3))
                        .parentPageId(resultSet.getLong(4))
                        .build();

                List<Long> subPages = new ArrayList<>();
                long subPageId = resultSet.getLong(5);
                if (subPageId != 0) subPages.add(subPageId);
                while (resultSet.next()) {
                    subPageId = resultSet.getLong(5);
                    subPages.add(subPageId);
                }
                vo.setSubPages(subPages);
                return Optional.of(vo);
            } else
                return Optional.empty();
        } catch (SQLException e) {
            log.error("db error occurred", e);
            throw new RuntimeException(e);
        } finally {
            close(con, statement, resultSet);
        }
    }

    private void close(Connection con, PreparedStatement statement) {
        JdbcUtils.closeStatement(statement);

        if (con != null) {
            try {
                con.setAutoCommit(true);
                con.close();
            } catch (Exception e) {
                log.info("error", e);
            }
        }
    }

    private void close(Connection con, PreparedStatement statement, ResultSet resultSet) {
        JdbcUtils.closeResultSet(resultSet);
        JdbcUtils.closeStatement(statement);

        if (con != null) {
            try {
                con.setAutoCommit(true);
                con.close();
            } catch (Exception e) {
                log.info("error", e);
            }
        }
    }
}
