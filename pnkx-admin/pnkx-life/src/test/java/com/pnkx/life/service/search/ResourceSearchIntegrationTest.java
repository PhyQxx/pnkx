package com.pnkx.life.service.search;

import com.pnkx.common.core.domain.entity.SysUser;
import com.pnkx.common.core.domain.model.LoginUser;
import com.pnkx.common.exception.ServiceException;
import com.pnkx.life.domain.po.PxNote;
import com.pnkx.life.domain.po.PxNoteFolder;
import com.pnkx.life.mapper.PxNoteFolderMapper;
import com.pnkx.life.mapper.PxNoteMapper;
import com.pnkx.life.service.NoteAccessService;
import com.pnkx.life.service.NoteRevisionConflictException;
import com.pnkx.life.service.impl.PxNoteFolderServiceImpl;
import com.pnkx.life.service.impl.PxNoteServiceImpl;
import com.pnkx.life.service.impl.PxOfflineSyncServiceImpl;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.junit.jupiter.api.Assumptions;
import java.sql.Connection;
import java.sql.DriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import javax.sql.DataSource;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

import com.pnkx.life.mapper.ResourceSearchMapper;
import com.pnkx.life.mapper.WorkspaceMapper;
import com.pnkx.life.mapper.ResourceLinkMapper;
import com.pnkx.life.mapper.ResourceTrashMapper;
import com.pnkx.life.mapper.OrganizeMapper;
import com.pnkx.life.mapper.TodoSeriesMapper;
import com.pnkx.life.mapper.UserFileMapper;
import com.pnkx.life.mapper.PxToDoMapper;
import com.pnkx.life.service.recurrence.*;
import com.pnkx.life.service.workspace.*;
import com.pnkx.framework.web.service.DataPermissionService;

class ResourceSearchIntegrationTest {
    private SqlSession session;
    private Connection mysqlAdmin;
    private String mysqlSchema;
    private DataSource dataSource;
    private SearchService service;
    private ResourceTagService tags;
    private WorkspaceMapper workspace;
    private WorkspaceQaStore qa;
    private ResourceLinkService links;
    private PxNoteServiceImpl noteService;
    private ResourceAccessService access;
    private ResourceConvertService conversions;
    private ResourceLifecycleService lifecycle;
    private OrganizeService organize;
    private TodoSeriesService series;
    private UserFileService files;
    private java.nio.file.Path fileRoot;
    private boolean allShared;
    private List<Long> shared = List.of(101L, 303L);

    @BeforeEach
    void setup() throws Exception {
        String mysqlUrl = System.getProperty("note.test.mysqlUrl");
        UnpooledDataSource ds;
        if (mysqlUrl != null) {
            if (!mysqlUrl.matches("jdbc:mysql://127\\.0\\.0\\.1:[0-9]+/")) {
                throw new IllegalArgumentException("Only the isolated loopback MySQL fixture is supported");
            }
            mysqlSchema = "pnkx_search_test_" + UUID.randomUUID().toString().replace("-", "");
            mysqlAdmin = DriverManager.getConnection(mysqlUrl, "root", "");
            try (Statement stmt = mysqlAdmin.createStatement()) {
                stmt.execute("CREATE DATABASE " + mysqlSchema);
            }
            ds = new UnpooledDataSource("com.mysql.cj.jdbc.Driver", mysqlUrl + mysqlSchema, "root", "");
        } else {
            ds = new UnpooledDataSource("org.h2.Driver", "jdbc:h2:mem:" + UUID.randomUUID()
                    + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        }
        dataSource = ds;
        Configuration config = new Configuration(new Environment("test", new SpringManagedTransactionFactory(), ds));

        try (InputStream in = getClass().getResourceAsStream("/mapper/ResourceSearchMapper.xml")) {
            new XMLMapperBuilder(in, config, "ResourceSearchMapper.xml", config.getSqlFragments()).parse();
        }
        config.getTypeAliasRegistry().registerAlias("PxNote", PxNote.class);
        config.getTypeAliasRegistry().registerAlias("PxNoteFolder", PxNoteFolder.class);
        for (String file : List.of("WorkspaceQaMapper.xml", "ResourceLinkMapper.xml", "ResourceTrashMapper.xml", "OrganizeMapper.xml", "TodoSeriesMapper.xml", "UserFileMapper.xml", "PxToDoMapper.xml", "PxNoteMapper.xml")) {
            try (InputStream in = getClass().getResourceAsStream("/mapper/" + file)) {
                new XMLMapperBuilder(in,config,file,config.getSqlFragments()).parse();
            }
        }
        try (InputStream in = getClass().getResourceAsStream("/mapper/WorkspaceMapper.xml")) {
            new XMLMapperBuilder(in, config, "WorkspaceMapper.xml", config.getSqlFragments()).parse();
        }
        var factory = new SqlSessionFactoryBuilder().build(config);
        session = factory.openSession(true);
        try (Statement stmt = session.getConnection().createStatement()) {
            String common = "id BIGINT AUTO_INCREMENT PRIMARY KEY, create_by VARCHAR(64), create_time TIMESTAMP, update_time TIMESTAMP, ";
            for (String table : List.of("px_article", "px_to_do", "px_diary", "px_note", "px_bookkeeping_record",
                    "px_commemoration_day", "px_subscription", "px_recipe", "px_shopping_list", "px_book", "px_web_article")) {
                stmt.execute("CREATE TABLE " + table + " (" + common + "read_flag INT DEFAULT 0, star_flag INT DEFAULT 0, del_flag INT DEFAULT 0, fetch_status VARCHAR(20), title VARCHAR(255), "
                    + "folder BIGINT, `order` INT, version VARCHAR(20), client_uuid VARCHAR(36), update_by VARCHAR(64), revision BIGINT DEFAULT 1, rich_text LONGTEXT, content_html LONGTEXT, content LONGTEXT, remark VARCHAR(255), tag VARCHAR(255), label VARCHAR(255), tags VARCHAR(255), "
                    + "name VARCHAR(255), notes LONGTEXT, description LONGTEXT, author VARCHAR(255), content_text LONGTEXT, "
                    + "performer VARCHAR(64), plan_start_time TIMESTAMP, status BOOLEAN DEFAULT FALSE, finish_by VARCHAR(64), finish_time TIMESTAMP, priority INT, kanban_status INT, sort_order INT, parent_id BIGINT, plan_end_time TIMESTAMP, date TIMESTAMP, pay_time TIMESTAMP, money DECIMAL(12,2), type BIGINT)");
            }
            stmt.execute("CREATE TABLE px_bookkeeping_classification(id BIGINT AUTO_INCREMENT PRIMARY KEY, create_by VARCHAR(64), type_name VARCHAR(255))");
        }
        String migration = java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("basedir"),
                "../pnkx-admin/src/main/resources/db/migration/V1.6.5__workspace_tags.sql"));
        try (Statement stmt = session.getConnection().createStatement()) {
            for (String sql : migration.split(";")) if (!sql.isBlank()) stmt.execute(sql);
        }
        String linkMigration = java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("basedir"),
                "../pnkx-admin/src/main/resources/db/migration/V1.6.6__workspace_links_state.sql"));
        try (Statement stmt = session.getConnection().createStatement()) {
            for (String sql : linkMigration.split(";")) if (!sql.isBlank()) stmt.execute(sql);
        }
        try (Statement stmt=session.getConnection().createStatement()) { stmt.execute("CREATE TABLE px_note_folder(id BIGINT PRIMARY KEY,create_by VARCHAR(64),del_flag INT DEFAULT 0)"); }
        workspace = new SqlSessionTemplate(factory).getMapper(WorkspaceMapper.class);
        service = new SearchService();
        inject(service, "workspaceMapper", workspace);
        inject(service, "mapper", new SqlSessionTemplate(factory).getMapper(ResourceSearchMapper.class));
        inject(service, "permissions", new DataPermissionService() {
            @Override public List<Long> getVisibleUserIds(String module) { return allShared ? null : shared; }
        });
        access = new ResourceAccessService();
        inject(access, "mapper", workspace);
        inject(access, "permissions", new DataPermissionService() {
            @Override public List<Long> getVisibleUserIds(String module) { return allShared ? null : shared; }
        });
        tags = new ResourceTagService();
        inject(tags, "mapper", workspace); inject(tags, "access", access);
        links = new ResourceLinkService();
        inject(links,"mapper",new SqlSessionTemplate(factory).getMapper(ResourceLinkMapper.class));
        inject(links,"workspace",workspace); inject(links,"access",access);
        noteService = new PxNoteServiceImpl();
        inject(noteService,"pxNoteMapper",new SqlSessionTemplate(factory).getMapper(PxNoteMapper.class));
        inject(noteService,"noteAccessService",new NoteAccessService()); inject(noteService,"resourceLinks",links);
        String historyMigration = java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("basedir"),
                "../pnkx-admin/src/main/resources/db/migration/V1.6.7__note_history.sql"));
        try (Statement stmt = session.getConnection().createStatement()) {
            for (String sql : historyMigration.split(";")) if (!sql.isBlank()) stmt.execute(sql);
        }
        String organizeMigration=java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("basedir"),"../pnkx-admin/src/main/resources/db/migration/V1.6.8__organize_suggestions.sql"));
        try(Statement stmt=session.getConnection().createStatement()) { for(String sql:organizeMigration.split(";")) if(!sql.isBlank()) stmt.execute(sql); }
        organize=new OrganizeService(); inject(organize,"noteMapper",new SqlSessionTemplate(factory).getMapper(PxNoteMapper.class)); inject(organize,"workspace",workspace); inject(organize,"mapper",new SqlSessionTemplate(factory).getMapper(OrganizeMapper.class));
        inject(organize,"access",access); inject(organize,"links",links); inject(organize,"notes",noteService);
        String seriesMigration=java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("basedir"),"../pnkx-admin/src/main/resources/db/migration/V1.6.9__todo_series.sql"));
        try(Statement stmt=session.getConnection().createStatement()) { for(String sql:seriesMigration.split(";")) if(!sql.isBlank()) stmt.execute(sql); }
        series=new TodoSeriesService();inject(series,"mapper",new SqlSessionTemplate(factory).getMapper(TodoSeriesMapper.class));
        inject(series,"todos",new SqlSessionTemplate(factory).getMapper(PxToDoMapper.class));
        inject(series,"reminders",java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{com.pnkx.life.service.IPxLifeReminderService.class},(proxy,method,args)->1));
        String fileMigration=java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("basedir"),"../pnkx-admin/src/main/resources/db/migration/V1.7.1__private_files.sql"));
        try(Statement stmt=session.getConnection().createStatement()) { for(String sql:fileMigration.split(";")) if(!sql.isBlank()) stmt.execute(sql); }
        files=new UserFileService();inject(files,"mapper",new SqlSessionTemplate(factory).getMapper(UserFileMapper.class));inject(files,"workspace",workspace);
        fileRoot=java.nio.file.Files.createTempDirectory("pnkx-private-file-test-").toRealPath();inject(files,"root",fileRoot.toString());
        conversions=new ResourceConvertService();
        inject(conversions,"access",access); inject(conversions,"workspace",workspace);
        inject(conversions,"actions",new SqlSessionTemplate(factory).getMapper(ResourceLinkMapper.class));
        inject(conversions,"links",links); inject(conversions,"notes",noteService);
        lifecycle=new ResourceLifecycleService(); inject(lifecycle,"files",files); inject(lifecycle,"notes",noteService);
        inject(lifecycle,"mapper",new SqlSessionTemplate(factory).getMapper(PxNoteMapper.class));
        inject(lifecycle,"trash",new SqlSessionTemplate(factory).getMapper(ResourceTrashMapper.class));
        String feedMigration=java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("basedir"),"../pnkx-admin/src/main/resources/db/migration/V1.7.2__note_change_feed.sql"));
        try(Statement stmt=session.getConnection().createStatement()){for(String sql:feedMigration.split(";"))if(!sql.isBlank())stmt.execute(sql);}
        String qaMigration=java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("basedir"),"../pnkx-admin/src/main/resources/db/migration/V1.7.3__workspace_qa.sql"));
        try(Statement stmt=session.getConnection().createStatement()){for(String sql:qaMigration.split(";"))if(!sql.isBlank())stmt.execute(sql);}
        qa=new WorkspaceQaStore();inject(qa,"mapper",new SqlSessionTemplate(factory).getMapper(com.pnkx.life.mapper.WorkspaceQaMapper.class));
        inject(qa,"workspace",workspace);inject(qa,"access",access);inject(qa,"links",links);inject(qa,"notes",noteService);
        login(101);
    }

    @Test
    void qaDraftRechecksSourcesAndAppliesOnceThroughNoteService() throws Exception {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        row("px_note",71,"101","正文证据","2026-10-11 10:00:00","");
        var source=access.read("note",71L);
        var citations=List.of(new WorkspaceQaStore.Citation("note","71","正文证据",1L,WorkspaceQaStore.hash(source),"text:0:4","正文证据"));
        tx.executeWithoutResult(status->qa.start("qa-test-request-01",100));
        assertThrows(ServiceException.class,()->tx.executeWithoutResult(status->qa.start("qa-test-request-02",100)));
        assertEquals(Boolean.TRUE,tx.execute(status->qa.finish("qa-test-request-01","done","根据 [资料 1] 的回答",citations,"已覆盖",25,18)));
        assertEquals(1,((List<?>)qa.detail("qa-test-request-01").get("citations")).size());
        var first=tx.execute(status->qa.apply("qa-test-request-01","note","确认的标题","确认的正文"));
        var retry=tx.execute(status->qa.apply("qa-test-request-01","note","重复点击","不能再建"));
        assertEquals(first,retry);assertEquals("确认的正文",access.read("note",Long.valueOf(first.get("id"))).getContent());
        assertEquals(1,links.outgoing("note",Long.valueOf(first.get("id"))).size());
        login(202);assertTrue(qa.list().isEmpty());assertThrows(ServiceException.class,()->qa.detail("qa-test-request-01"));
        login(101);try(var stmt=session.getConnection().createStatement()){stmt.execute("update px_note set content='已改变' where id=71");}
        assertThrows(ServiceException.class,()->qa.detail("qa-test-request-01"));
    }

    @Test
    void qaCancellationRecordsPartialUsageAndCannotBecomeDraft() throws Exception {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        tx.executeWithoutResult(status->qa.start("qa-cancel-request-01",150));
        login(202);qa.cancel("qa-cancel-request-01");assertFalse(qa.cancelled("101","qa-cancel-request-01"));
        login(101);qa.cancel("qa-cancel-request-01");assertTrue(qa.cancelled("101","qa-cancel-request-01"));
        assertEquals(Boolean.FALSE,tx.execute(status->qa.finish("qa-cancel-request-01","done","不应保存",List.of(),"",20,8)));
        tx.execute(status->qa.finish("qa-cancel-request-01","failed",null,List.of(),"",20,8));
        assertEquals("cancelled",qa.detail("qa-cancel-request-01").get("status"));
        assertEquals(8,qa.list().getFirst().getOutputChars());
        assertThrows(ServiceException.class,()->tx.execute(status->qa.apply("qa-cancel-request-01","note","标题","正文")));
        assertThrows(ServiceException.class,()->tx.executeWithoutResult(status->qa.start("qa-cancel-request-01",150)));
    }

    @Test
    void cancellationBeforeStartPreventsLateGeneration() {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        tx.executeWithoutResult(status->qa.cancel("qa-early-cancel-01"));
        assertThrows(ServiceException.class,()->tx.executeWithoutResult(status->qa.start("qa-early-cancel-01",100)));
        assertEquals("cancelled",qa.detail("qa-early-cancel-01").get("status"));
    }

    @Test
    void qaSourcePermissionsAreRevalidatedAfterSharingIsRevoked() throws Exception {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        row("px_to_do",81,"303","共享待办","2026-10-11 10:00:00","");
        try(var stmt=session.getConnection().createStatement()){stmt.execute("update px_to_do set remark='真正的待办说明' where id=81");}
        var source=access.read("todo",81L);assertEquals("真正的待办说明",WorkspaceQaStore.plain(source));
        var citations=List.of(new WorkspaceQaStore.Citation("todo","81",source.getTitle(),0L,WorkspaceQaStore.hash(source),"text:0:8",source.getContent()));
        tx.executeWithoutResult(status->qa.start("qa-share-request-01",100));
        tx.execute(status->qa.finish("qa-share-request-01","done","回答",citations,"已覆盖",20,2));
        shared=List.of(101L);
        assertThrows(ServiceException.class,()->qa.detail("qa-share-request-01"));
        assertThrows(ServiceException.class,()->tx.execute(status->qa.apply("qa-share-request-01","note","标题","正文")));
    }

    private void row(String table, int id, String owner, String title, String date, String tags) throws Exception {
        try (var stmt = session.getConnection().prepareStatement("INSERT INTO " + table +
                " (id,create_by,title,content,name,label,tags,tag,create_time,update_time) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            stmt.setInt(1, id); stmt.setString(2, owner); stmt.setString(3, title); stmt.setString(4, title);
            stmt.setString(5, title); stmt.setString(6, tags); stmt.setString(7, tags); stmt.setString(8, tags);
            stmt.setString(9, date); stmt.setString(10, date); stmt.executeUpdate();
        }
    }

    private UserFile uploadFile(Long parent,String name,byte[] content,boolean broken) {
        org.springframework.web.multipart.MultipartFile part=new org.springframework.web.multipart.MultipartFile() {
            public String getName(){return "file";} public String getOriginalFilename(){return name;}
            public String getContentType(){return "text/html";} public boolean isEmpty(){return content.length==0;}
            public long getSize(){return broken?content.length+5:content.length;} public byte[] getBytes(){return content;}
            public java.io.InputStream getInputStream(){return new java.io.ByteArrayInputStream(content);}
            public void transferTo(java.io.File dest) throws java.io.IOException{java.nio.file.Files.write(dest.toPath(),content);}
        };
        try{return files.upload(parent,part,null);}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
    }

    @Test
    void clipReadAndStarFiltersApplyBeforePagination() throws Exception {
        for(int i=1;i<=12;i++)row("px_web_article",i,"101","clip","2026-01-01 00:00:00","");
        try(var stmt=session.getConnection().createStatement()){stmt.execute("update px_web_article set read_flag=1,star_flag=1 where id=12");}
        var result=service.workspaceFiltered(null,"webArticle",null,null,null,"all",false,false,null,false,1,10,true,true);
        assertEquals(1,result.total());assertEquals("12",result.rows().getFirst().getId());
        assertThrows(ServiceException.class,()->service.workspaceFiltered(null,"note",null,null,null,"all",false,false,null,false,1,10,true,null));
    }

    @Test
    void schedulerRegistrationMigrationIsRepeatable() throws Exception {
        String migration=java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("basedir"),"../pnkx-admin/src/main/resources/db/migration/V1.7.0__todo_series_job.sql"));
        try(var stmt=session.getConnection().createStatement()){
            stmt.execute("CREATE TABLE sys_job(job_name VARCHAR(100),job_group VARCHAR(30),invoke_target VARCHAR(100),cron_expression VARCHAR(100),misfire_policy VARCHAR(10),concurrent VARCHAR(10),status VARCHAR(10),remark VARCHAR(255),create_by VARCHAR(50),create_time TIMESTAMP)");
            stmt.execute(migration);stmt.execute(migration);
            try(var result=stmt.executeQuery("SELECT COUNT(*) FROM sys_job")){result.next();assertEquals(1,result.getInt(1));}
        }
    }

    @Test
    void privateFilesEnforceOwnershipQuotaAndTransactionalCleanup() throws Exception {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        UserFile file=tx.execute(status->uploadFile(0L,"safe.txt","private content".getBytes(),false));
        assertEquals("text/plain",file.getMime());assertTrue(java.nio.file.Files.exists(files.download(file)));
        assertThrows(ServiceException.class,()->tx.execute(status->uploadFile(0L,"../escape.txt",new byte[]{1},false)));
        assertThrows(ServiceException.class,()->tx.execute(status->uploadFile(0L,"incomplete.txt",new byte[]{1},true)));
        try(var paths=java.nio.file.Files.list(fileRoot)){assertEquals(1L,paths.count());}
        inject(files,"quota",15L);assertThrows(ServiceException.class,()->tx.execute(status->uploadFile(0L,"full.txt",new byte[]{1,2},false)));
        login(202);assertThrows(ServiceException.class,()->files.get(file.getId()));
        assertEquals(0,service.search("safe","file",null,null,null,1,20).total());
    }

    @Test
    void fileFoldersRejectCyclesAndDeletedFilesDisappearFromSearchAndLinks() throws Exception {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        UserFile folder=tx.execute(status->files.folder(0L,"folder"));UserFile child=tx.execute(status->files.folder(folder.getId(),"child"));
        assertThrows(ServiceException.class,()->tx.execute(status->files.edit(folder.getId(),1L,child.getId(),"folder")));
        UserFile file=tx.execute(status->uploadFile(folder.getId(),"readme.txt","text".getBytes(),false));
        var tag=tag("files",0L);tags.replace("file",file.getId(),List.of(tag.getId()));
        row("px_note",1,"101","note","2026-01-01 00:00:00","");links.add("note",1L,"file",file.getId());
        assertEquals(1,service.search("readme","file",null,null,"files",1,20).total());
        tx.executeWithoutResult(status->files.delete(file.getId(),1L));
        assertTrue(links.outgoing("note",1L).getFirst().isUnavailable());assertEquals(0,service.search("readme","file",null,null,null,1,20).total());
        tx.executeWithoutResult(status->files.delete(child.getId(),1L));tx.executeWithoutResult(status->files.delete(folder.getId(),1L));
        tx.executeWithoutResult(status->lifecycle.restore("file",file.getId()));
        assertEquals(0L,files.get(file.getId()).getParentId());assertFalse(links.outgoing("note",1L).getFirst().isUnavailable());
    }

    @Test
    void recurringInstancesAreUniqueCatchUpIsBoundedAndSeriesEditsUseCas() throws Exception {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        TodoSeries input=new TodoSeries();input.setTitle("recurring");input.setRuleType("daily");input.setStartDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).minusDays(40).toString());
        TodoSeries saved=tx.execute(status->series.save(input,"future"));
        assertEquals(31,tx.execute(status->series.generate(saved.getId())).intValue());
        assertEquals(10,tx.execute(status->series.generate(saved.getId())).intValue());
        assertEquals(0,tx.execute(status->series.generate(saved.getId())).intValue());
        assertEquals(41,service.workspace(null,"todo",null,null,null,"all",false,false,1,100).total());
        TodoSeries stale=series.list().getFirst(); saved.setPaused(true);tx.execute(status->series.save(saved,"future"));
        assertThrows(ServiceException.class,()->tx.execute(status->series.save(stale,"future")));
        assertEquals(0,tx.execute(status->series.generate(saved.getId())).intValue());
        login(202); assertThrows(ServiceException.class,()->tx.execute(status->series.save(saved,"all")));
    }

    @Test
    void deletingCompletionOccurrenceSkipsItWithoutBlockingSeries() throws Exception {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        TodoSeries input=new TodoSeries();input.setTitle("skip");input.setRuleType("completion");input.setStartDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).minusDays(2).toString());
        var saved=tx.execute(status->series.save(input,"future"));
        assertEquals(1,tx.execute(status->series.generate(saved.getId())).intValue());
        try(var stmt=session.getConnection().createStatement()){stmt.execute("delete from px_to_do where id="+series.list().getFirst().getLastTodoId());}
        assertEquals(0,tx.execute(status->series.generate(saved.getId())).intValue());
        var advanced=series.list().getFirst();assertFalse(advanced.getWaitingCompletion());
        assertEquals(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).plusDays(input.getIntervalDays()).toString(),advanced.getNextDate());
    }

    @Test
    void completionRecurrenceWaitsForActualCompletion() throws Exception {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        TodoSeries input=new TodoSeries();input.setTitle("completion");input.setRuleType("completion");input.setStartDate(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Shanghai")).minusDays(2).toString());
        TodoSeries saved=tx.execute(status->series.save(input,"future"));
        assertEquals(1,tx.execute(status->series.generate(saved.getId())).intValue());
        assertEquals(0,tx.execute(status->series.generate(saved.getId())).intValue());
        try(var stmt=session.getConnection().createStatement()) { stmt.execute("update px_to_do set status=true,finish_time='"+java.time.LocalDate.now().minusDays(1)+" 00:00:00'"); }
        assertEquals(1,tx.execute(status->series.generate(saved.getId())).intValue());
        assertEquals(0,tx.execute(status->series.generate(saved.getId())).intValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void organizeIsPrivateIdempotentAndRejectsChangedSnapshots() throws Exception {
        row("px_note",1,"101","","2026-01-01 00:00:00","");
        row("px_note",2,"202","private","2026-01-01 00:00:00","");
        try(var stmt=session.getConnection().createStatement()) { stmt.execute("update px_note set content='proposed title' where id=1"); }
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        tx.execute(status->organize.scan("note",0)); tx.execute(status->organize.scan("note",0));
        var rows=(List<OrganizeSuggestion>)organize.list(0).get("rows");
        assertEquals(2,rows.size());
        var title=rows.stream().filter(row->row.getRuleCode().equals("title")).findFirst().orElseThrow();
        assertEquals("applied",tx.execute(status->organize.apply(title.getId(),false)));
        assertEquals("applied",tx.execute(status->organize.apply(title.getId(),false)));
        assertEquals("proposed title",noteService.selectPxNoteById(1L).getTitle());
        var old=rows.stream().filter(row->row.getRuleCode().equals("untagged")).findFirst().orElseThrow();
        assertEquals("stale",tx.execute(status->organize.apply(old.getId(),false)));
        login(202); assertThrows(ServiceException.class,()->tx.execute(status->organize.apply(title.getId(),false)));
    }

    @Test
    void legacyTagAuditReportsDriftWithoutMutating() throws Exception {
        row("px_web_article",1,"101","clip","2026-01-01 00:00:00","legacy");
        assertEquals(1,((List<?>)tags.audit("webArticle",0).get("differences")).size());
        tags.backfill("webArticle");
        assertEquals(0,((List<?>)tags.audit("webArticle",0).get("differences")).size());
        login(202);assertEquals(0,tags.audit("webArticle",0).get("checked"));
    }

    @Test
    void conversionIsIdempotentAndRejectsKeyReuseOrForeignSource() throws Exception {
        row("px_web_article",1,"101","source","2026-01-01 00:00:00","");
        row("px_web_article",2,"202","private","2026-01-01 00:00:00","");
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        var first=tx.execute(status -> conversions.convert(1L,"note","conversion-request-001","copy","<unsafe>"));
        var again=tx.execute(status -> conversions.convert(1L,"note","conversion-request-001","copy","<unsafe>"));
        assertEquals(first,again);
        long id=Long.parseLong(first.get("id").toString());
        assertTrue(noteService.selectPxNoteById(id).getRichText().contains("&lt;unsafe&gt;"));
        assertEquals(2,links.outgoing("note",id).size());
        assertThrows(ServiceException.class,()->tx.execute(status -> conversions.convert(2L,"note","conversion-request-001","copy","")));
        assertThrows(ServiceException.class,()->tx.execute(status -> conversions.convert(1L,"todo","conversion-request-001","copy","")));
        assertEquals(1,service.workspace(null,"note",null,null,null,"all",false,false,1,20).total());
    }

    @Test
    void historyRetains50ChangesAndRestoresWithCasWithoutChangingFolder() throws Exception {
        row("px_note",1,"101","first","2026-01-01 00:00:00","");
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        for(int i=0;i<55;i++) {
            PxNote note=noteService.selectPxNoteById(1L); note.setContent("change-"+i);
            tx.execute(status -> noteService.updatePxNote(note));
        }
        assertEquals(50,lifecycle.history(1L).size());
        assertEquals(55L,lifecycle.history(1L).getFirst().getRevision());
        assertThrows(ServiceException.class,()->lifecycle.historical(1L,1L));
        assertThrows(NoteRevisionConflictException.class,()->tx.execute(status->lifecycle.restoreVersion(1L,55L,1L)));
        PxNote restored=tx.execute(status->lifecycle.restoreVersion(1L,55L,56L));
        assertEquals(57L,restored.getRevision()); assertEquals("change-53",restored.getContent());
        login(202); assertThrows(ServiceException.class,()->lifecycle.history(1L));
    }

    @Test
    void trashIsPrivateAndRestoreFallsBackToRootAndUpdatesSyncRevision() throws Exception {
        row("px_note",1,"101","own","2026-01-01 00:00:00","");
        row("px_note",2,"202","private","2026-01-01 00:00:00","");
        row("px_web_article",1,"101","clip","2026-01-01 00:00:00","");
        try(var stmt=session.getConnection().createStatement()) {
            stmt.execute("update px_note set del_flag=1,folder=999");
            stmt.execute("update px_web_article set del_flag=1");
        }
        assertEquals(2,lifecycle.trash(1,20).total());
        assertThrows(ServiceException.class,()->lifecycle.restore("note",2L));
        var tx=new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        tx.executeWithoutResult(status->lifecycle.restore("note",1L));
        assertEquals(0L,noteService.selectPxNoteById(1L).getFolder());
        assertEquals(2L,noteService.selectPxNoteById(1L).getRevision());
        tx.executeWithoutResult(status->lifecycle.restore("webArticle",1L));
        assertEquals(0,lifecycle.trash(1,20).total());
    }

    @Test
    void filtersBeforeLimitFindsTheEleventhCandidate() throws Exception {
        for (int i = 1; i <= 12; i++) row("px_note", i, "101", "needle", "2026-10-10 12:00:00", "");
        row("px_note", 99, "101", "needle old match", "2026-01-01 23:59:59", "");
        var page = service.search("needle", "note", "2026-01-01", "2026-01-01", null, 1, 10);
        assertEquals(1, page.total());
        assertEquals("99", page.rows().getFirst().getId());
    }

    @Test
    void combinedTypesHaveStablePaginationAndPrivateScopeEvenForAdmin() throws Exception {
        row("px_note", 1, "101", "match", "2026-01-01 00:00:00", "");
        row("px_diary", 1, "101", "match", "2026-01-01 00:00:00", "");
        row("px_web_article", 1, "101", "match", "2026-01-01 00:00:00", "");
        row("px_note", 2, "202", "match secret", "2026-01-01 00:00:00", "");
        allShared = true;
        var first = service.search("match", "all", null, null, null, 1, 2);
        var second = service.search("match", "all", null, null, null, 2, 2);
        assertEquals(3, first.total());
        assertTrue(first.hasMore());
        assertFalse(second.hasMore());
        assertEquals(List.of("diary", "note", "webArticle"), Stream.concat(first.rows().stream(), second.rows().stream())
                .map(SearchResult::getType).toList());
        assertTrue(second.rows().getFirst().getAppRoute().contains("webArticle/detail?id=1"));
    }

    @Test
    void moduleSharingIsExplicitAndEmptyScopeFailsClosed() throws Exception {
        row("px_to_do", 1, "303", "shared", "2026-01-01 00:00:00", "java");
        row("px_to_do", 2, "202", "shared secret", "2026-01-01 00:00:00", "java");
        assertEquals(1, service.search("shared", "todo", null, null, "java", 1, 10).total());
        shared = List.of();
        assertEquals(0, service.search("shared", "todo", null, null, null, 1, 10).total());
    }

    @Test
    void tagsAreExactTokensAndFilterBeforePagination() throws Exception {
        for (int i = 1; i <= 12; i++) row("px_to_do", i, "101", "match", "2026-10-01 00:00:00", "javascript");
        row("px_to_do", 99, "101", "match", "2026-01-01 00:00:00", "java,reading");
        assertEquals("99", service.search("match", "todo", null, null, "JAVA", 1, 10).rows().getFirst().getId());
        assertEquals(1, service.search("match", "todo", null, null, "java", 1, 10).total());
    }

    @Test
    void literalWildcardsDeletionAndEndDateBoundaries() throws Exception {
        row("px_note", 1, "101", "100%_value", "2026-01-01 23:59:59", "");
        row("px_note", 2, "101", "100xxvalue", "2026-01-01 12:00:00", "");
        row("px_note", 3, "101", "100%_value", "2026-01-02 00:00:00", "");
        row("px_note", 4, "101", "100%_value", "2026-01-01 12:00:00", "");
        try (Statement stmt = session.getConnection().createStatement()) { stmt.execute("UPDATE px_note SET del_flag=1 WHERE id=4"); }
        assertEquals("1", service.search("%_", "note", null, "2026-01-01", null, 1, 10).rows().getFirst().getId());
        assertEquals(1, service.search("%_", "note", null, "2026-01-01", null, 1, 10).total());
    }

    @Test
    void rejectsUntrustedTypeDatePaginationAndAnonymousAccess() {
        assertThrows(ServiceException.class, () -> service.search("q", "px_note;drop", null, null, null, 1, 10));
        assertThrows(ServiceException.class, () -> service.search("q", "note", "2026-02-30", null, null, 1, 10));
        assertThrows(ServiceException.class, () -> service.search("q", "note", null, null, null, 0, 10));
        SecurityContextHolder.clearContext();
        assertThrows(ServiceException.class, () -> service.search("q", "note", null, null, null, 1, 10));
    }
    private ResourceTag tag(String name, Long parent) {
        ResourceTag input = new ResourceTag(); input.setName(name); input.setParentId(parent);
        return tags.save(input);
    }

    @Test
    void tagsCannotLeakAcrossOwnersOrAttachToSharedResources() throws Exception {
        ResourceTag own = tag("private", 0L);
        login(202); ResourceTag other = tag("private", 0L); login(101);
        row("px_note", 1, "101", "mine", "2026-01-01 00:00:00", "");
        row("px_to_do", 2, "303", "shared", "2026-01-01 00:00:00", "");
        assertThrows(ServiceException.class, () -> tags.replace("note", 1L, List.of(other.getId())));
        assertThrows(ServiceException.class, () -> tags.replace("todo", 2L, List.of(own.getId())));
        assertEquals(List.of(own.getId()), tags.list().stream().map(ResourceTag::getId).toList());
    }

    @Test
    void tagCyclesAndStaleEditsAreRejected() {
        ResourceTag parent = tag("parent", 0L), child = tag("child", parent.getId());
        parent.setParentId(child.getId());
        assertThrows(ServiceException.class, () -> tags.save(parent));
        parent.setParentId(0L); parent.setName("renamed"); tags.save(parent);
        assertThrows(ServiceException.class, () -> tags.save(parent));
    }

    @Test
    void tagRenameMergeAndDeleteUpdateLegacyProjection() throws Exception {
        row("px_web_article", 1, "101", "clip", "2026-01-01 00:00:00", "");
        ResourceTag source = tag("Source", 0L), target = tag("Target", 0L);
        tags.replace("webArticle", 1L, List.of(source.getId()));
        source.setName("Renamed"); tags.save(source);
        assertEquals("Renamed", workspace.resource("webArticle", 1L, List.of(101L), false, false).getTags());
        tags.merge(source.getId(), target.getId());
        assertEquals(List.of(target.getId()), tags.forResource("webArticle", 1L));
        assertEquals("Target", workspace.resource("webArticle", 1L, List.of(101L), false, false).getTags());
        tags.remove(target.getId());
        assertEquals("", workspace.resource("webArticle", 1L, List.of(101L), false, false).getTags());
    }

    @Test
    void descendantAndOrFiltersArePermissionScopedAndCountedBeforePagination() throws Exception {
        row("px_note", 1, "101", "one", "2026-01-01 00:00:00", "");
        row("px_note", 2, "101", "two", "2026-01-01 00:00:00", "");
        ResourceTag parent = tag("Parent", 0L), child = tag("Child", parent.getId()), another = tag("Other", 0L);
        tags.replace("note", 1L, List.of(child.getId(), another.getId()));
        tags.replace("note", 2L, List.of(another.getId()));
        assertEquals(1, service.workspace("", "all", null, null, List.of(parent.getId(), another.getId()), "all", true, false, 1, 1).total());
        assertEquals(2, service.workspace("", "all", null, null, List.of(parent.getId(), another.getId()), "any", true, false, 1, 1).total());
        assertEquals(0, service.workspace("", "all", null, null, List.of(), "all", false, true, 1, 20).total());
        assertEquals(1, service.search("one", "note", null, null, "Child", 1, 20).total());
    }

    @Test
    void legacyBackfillIsResumableAndNewWritesStayConsistent() throws Exception {
        row("px_to_do", 1, "101", "one", "2026-01-01 00:00:00", " Java , java,阅读");
        row("px_to_do", 2, "202", "secret", "2026-01-01 00:00:00", "private");
        assertEquals(1, tags.backfill("todo").get("processed"));
        assertEquals(0, tags.backfill("todo").get("processed"));
        assertEquals(2, tags.list().size());
        tags.syncLegacy("101", "todo", 1L, "new");
        assertEquals(1, tags.forResource("todo", 1L).size());
        assertEquals("new", workspace.resource("todo", 1L, List.of(101L), false, false).getTags());
    }

    @Test
    void referencesAreRedactedWhenAccessIsRevokedAndBacklinksFilterSources() throws Exception {
        row("px_note", 1, "101", "source", "2026-01-01 00:00:00", "");
        row("px_to_do", 2, "303", "secret target", "2026-01-01 00:00:00", "");
        links.add("note",1L,"todo",2L);
        assertEquals("secret target",links.outgoing("note",1L).getFirst().getTitle());
        shared = List.of(101L);
        assertTrue(links.outgoing("note",1L).getFirst().isUnavailable());
        assertFalse(links.outgoing("note",1L).getFirst().getTitle().contains("secret"));
        shared = List.of(101L,303L);
        try (Statement stmt = session.getConnection().createStatement()) { stmt.execute("UPDATE px_note SET create_by='202' WHERE id=1"); }
        assertTrue(links.incoming("todo",2L).isEmpty());
    }

    @Test
    void htmlReferencesIgnoreCodeAndFailedRelationRollsBackTheWholeNoteSave() throws Exception {
        row("px_note", 1, "101", "old", "2026-01-01 00:00:00", "");
        row("px_note", 2, "202", "secret", "2026-01-01 00:00:00", "");
        row("px_web_article", 3, "101", "article", "2026-01-01 00:00:00", "");
        var tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        PxNote draft = noteService.selectPxNoteById(1L);
        draft.setContent("changed"); draft.setRichText("<a href='/r/note/2'>forbidden</a>");
        assertThrows(ServiceException.class, () -> tx.execute(status -> noteService.updatePxNote(draft)));
        assertEquals("old", noteService.selectPxNoteById(1L).getContent());
        assertEquals(1L, noteService.selectPxNoteById(1L).getRevision());
        draft.setRichText("<code><a href='/r/note/2'>example</a></code><a href='/r/webArticle/3'>source</a>");
        tx.execute(status -> noteService.updatePxNote(draft));
        assertEquals(1, links.outgoing("note",1L).size());
        assertEquals(3L, links.outgoing("note",1L).getFirst().getTargetId());
        draft.setRevision(2L); draft.setRichText("<p>no references</p>");
        tx.execute(status -> noteService.updatePxNote(draft));
        assertTrue(links.outgoing("note",1L).isEmpty());
    }

    @Test
    void recentAndInboxNeverExposeDeletedOrOtherUsersResources() throws Exception {
        row("px_note", 1, "101", "visited", "2026-01-01 00:00:00", "");
        row("px_note", 2, "202", "private", "2026-01-01 00:00:00", "");
        links.visit("note",1L); links.inbox("note",1L,true);
        assertEquals(1, service.workspace("","all",null,null,List.of(),"all",false,false,true,true,1,10).total());
        assertThrows(ServiceException.class, () -> links.visit("note",2L));
        try (Statement stmt = session.getConnection().createStatement()) { stmt.execute("UPDATE px_note SET del_flag=1 WHERE id=1"); }
        assertEquals(0, service.workspace("","all",null,null,List.of(),"all",false,false,true,true,1,10).total());
    }

    @Test
    void legacyProjectionFailureRollsBackNewTagRelations() throws Exception {
        row("px_to_do",1,"101","todo","2026-01-01 00:00:00","");
        var tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        assertThrows(ServiceException.class, () -> tx.execute(status -> {
            tags.syncLegacy("101","todo",1L,"a".repeat(100) + "," + "b".repeat(100)); return null;
        }));
        assertEquals(0,tags.list().size());
        assertEquals(0,tags.forResource("todo",1L).size());
    }

    @AfterEach
    void cleanup() {
        if(fileRoot!=null)try(var paths=java.nio.file.Files.walk(fileRoot)){for(var path:paths.sorted(java.util.Comparator.reverseOrder()).toList())java.nio.file.Files.delete(path);}catch(Exception ex){throw new RuntimeException(ex);}
        SecurityContextHolder.clearContext();
        if (session != null) session.close();
        if (mysqlAdmin != null) {
            try (Connection admin = mysqlAdmin; Statement stmt = mysqlAdmin.createStatement()) {
                stmt.execute("DROP DATABASE " + mysqlSchema);
            } catch (Exception ex) { throw new RuntimeException(ex); }
        }
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void login(long id) {
        SysUser user = new SysUser();
        user.setUserId(id);
        user.setUserName("test-" + id);
        LoginUser login = new LoginUser(user, Set.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login, null, List.of()));
    }
}
