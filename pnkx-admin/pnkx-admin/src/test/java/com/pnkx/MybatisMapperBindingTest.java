package com.pnkx;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * MyBatis Mapper 绑定完整性验证。
 * <p>
 * 背景：包重命名/模块拆分时，Java 接口与 mapper XML 的 namespace、语句 id、
 * 类型别名的错位只会在应用启动或首次调用时以 "Invalid bound statement"
 * 或 BindingException 暴露，编译期完全无感。
 * 本测试不连数据库、不启动 Spring，把 classpath 上全部 mapper XML 解析进
 * 真实 MyBatis Configuration，逐一断言：
 * <ol>
 *   <li>每个 XML 的 namespace 都能加载为接口类；</li>
 *   <li>接口的每个方法都有对应的 MappedStatement（XML 语句缺失即启动期必炸）；</li>
 *   <li>解析出的 resultMap 数量符合预期（类型/别名解析失败会在 parse 阶段直接报错）。</li>
 * </ol>
 * 别名注册范围与 application.yml 的 type-aliases-package（com.pnkx.**.domain）同构。
 *
 * @author phy
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MybatisMapperBindingTest {

    private Configuration configuration;

    /**
     * 与 application.yml 的 type-aliases-package（com.pnkx.**.domain）等价的
     * 全部 domain 包：各业务模块 + common/system/framework/generator
     */
    private static final List<String> DOMAIN_PACKAGES = Arrays.asList(
            "com.pnkx.life.domain", "com.pnkx.blog.domain", "com.pnkx.chat.domain",
            "com.pnkx.material.domain", "com.pnkx.ai.domain", "com.pnkx.system.domain",
            "com.pnkx.common.core.domain", "com.pnkx.framework.web.domain", "com.pnkx.generator.domain",
            "com.pnkx.quartz.domain");

    @BeforeAll
    void parseAllMapperXml() throws Exception {
        configuration = new Configuration();
        for (String pkg : DOMAIN_PACKAGES) {
            configuration.getTypeAliasRegistry().registerAliases(pkg);
        }

        Set<String> parsed = new HashSet<>();
        Enumeration<URL> resources = getClass().getClassLoader().getResources("mapper");
        while (resources.hasMoreElements()) {
            URL mapperDir = resources.nextElement();
            String dir = mapperDir.toString();
            // reactor 构建的 target/classes 或依赖 jar 内的 mapper 目录
            if (dir.contains("/target/classes/") || dir.endsWith("!/mapper")) {
                scanRecursively(mapperDir, parsed);
            }
        }
        assertTrue(parsed.size() >= 60, "应解析到全部业务 mapper XML，实际只解析到 " + parsed.size() + " 个");
    }

    private void scanRecursively(URL dirUrl, Set<String> parsed) {
        java.nio.file.Path path;
        try {
            path = java.nio.file.Paths.get(dirUrl.toURI());
        } catch (Exception e) {
            // jar 内资源走不到 Path，跳过（reactor 测试场景主要来自 target/classes）
            return;
        }
        try (var walk = java.nio.file.Files.walk(path)) {
            walk.filter(p -> p.toString().endsWith("Mapper.xml")).forEach(xml -> {
                try (InputStream in = java.nio.file.Files.newInputStream(xml)) {
                    new XMLMapperBuilder(in, configuration, xml.toString(), configuration.getSqlFragments()).parse();
                    parsed.add(xml.toString());
                } catch (Exception e) {
                    fail("解析 mapper XML 失败: " + xml + " -> " + e);
                }
            });
        } catch (Exception e) {
            fail("遍历 mapper 目录失败: " + dirUrl + " -> " + e);
        }
    }

    @Test
    void 每个mapper接口方法都有绑定语句() {
        Set<String> namespaces = new HashSet<>();
        for (String id : configuration.getMappedStatementNames()) {
            // StrictMap 同时登记全限定 id 与短名（无点号），只取全限定的
            if (id.indexOf('.') > 0) {
                namespaces.add(id.substring(0, id.lastIndexOf('.')));
            }
        }

        int checked = 0;
        for (String namespace : namespaces) {
            Class<?> mapperClass;
            try {
                mapperClass = Class.forName(namespace);
            } catch (ClassNotFoundException e) {
                fail("mapper XML 的 namespace 不是可加载的类: " + namespace);
                return;
            }
            assertTrue(mapperClass.isInterface(), "namespace 应为接口: " + namespace);
            for (Method method : mapperClass.getMethods()) {
                // 跳过 default 方法与父接口（如 mybatis-plus BaseMapper）声明的方法
                if (method.isDefault() || method.getDeclaringClass() != mapperClass) {
                    continue;
                }
                String statementId = namespace + "." + method.getName();
                try {
                    configuration.getMappedStatement(statementId);
                } catch (Exception e) {
                    fail(mapperClass.getSimpleName() + "." + method.getName()
                            + " 没有对应的 XML 语句（启动期将抛 Invalid bound statement）");
                }
                checked++;
            }
        }
        assertTrue(checked > 200, "应校验到足量的 mapper 方法，实际 " + checked);
    }

    @Test
    void resultMap规模符合预期() {
        int resultMaps = configuration.getResultMaps().size();
        assertTrue(resultMaps > 40, "应加载足量 resultMap（类型/别名解析失败会在 parse 阶段报错），实际 " + resultMaps);
    }
}
