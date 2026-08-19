package com.lanou.springaidemo;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.model.ClassAnnotationAttributes;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;

import java.sql.Types;
import java.util.Collections;

public class CodeGenerator {
    private static final String URL = "jdbc:mysql://localhost:3306/db_blog";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";
    public static void main(String[] args) {

        FastAutoGenerator.create(URL, USER, PASSWORD)
                .globalConfig(builder -> {
                    builder.author("lanou") // 设置作者
                            .enableSpringdoc() // 开启 swagger 模式
                            .outputDir("src\\main\\java"); // 指定输出目录
                })
                .dataSourceConfig(builder ->
                        builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                            int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                            if (typeCode == Types.SMALLINT) {
                                // 自定义类型转换
                                return DbColumnType.INTEGER;
                            }
                            return typeRegistry.getColumnType(metaInfo);
                        })
                )
                .packageConfig(builder ->
                        builder.parent("com.lanou.springaidemo") // 设置父包名
                                //.moduleName("system") // 设置父包模块名
                                .pathInfo(Collections.singletonMap(OutputFile.xml, "src\\main\\resources\\mappers")) // 设置mapperXml生成路径
                )
                .strategyConfig(builder ->
                        builder.addInclude("t_comments","t_likes","t_posts","t_tags","t_users") // 设置需要生成的表名
                                .addTablePrefix("t_", "c_") // 设置过滤表前缀
                                .entityBuilder().enableLombok(new ClassAnnotationAttributes(Data.class))
                                .controllerBuilder().enableRestStyle()
                                .serviceBuilder().convertServiceFileName(entityName ->{
                                    return entityName + "Service";
                                })
                                .mapperBuilder().mapperAnnotation(Mapper.class)
                )
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();
    }
}
