package org.gh;

import org.gh.jooq.tables.Reader;
import org.gh.jooq.tables.ReaderBook;
import org.gh.jooq.tables.records.AuthorRecord;
import org.gh.jooq.tables.records.ReaderRecord;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.gh.jooq.Tables.*;
import static org.jooq.Records.mapping;
import static org.jooq.impl.DSL.*;

@SpringBootTest
public class JooqTest {

    @Autowired
    private DSLContext db;

    @Test
    void t100_simple() {
        db
//                .select()
//                .select(AUTHOR.asterisk())
                .select(AUTHOR.NAME, AUTHOR.BIRTH_YEAR)
                .from(AUTHOR)
//                .where(AUTHOR.BIRTH_YEAR.lt(1900).and(AUTHOR.NAME.like("J%")))
//                .orderBy(AUTHOR.BIRTH_YEAR.desc())
                .orderBy(2)
                .limit(2)
                .offset(1)
                .fetch();
    }

    @Test
    void t300_forEachLoop() {
        for (var r : db
                .select(AUTHOR.NAME, AUTHOR.BIRTH_YEAR)
                .from(AUTHOR)
                .fetchSize(1)
                .fetchLazy()
        ) {
            System.out.printf("%s born %s%n", r.get(AUTHOR.NAME), r.get(AUTHOR.BIRTH_YEAR));
        }
    }

    @Test
    void t400_fetchStream() {
        db
                .select(AUTHOR.NAME, AUTHOR.BIRTH_YEAR)
                .from(AUTHOR)
//                .fetchStream()
//                .forEach(r -> System.out.printf("%s born %s%n", r.value1(), r.value2()));
                .forEach(r -> System.out.printf("%s born %s%n", r.get(AUTHOR.NAME), r.get(AUTHOR.BIRTH_YEAR)));
    }

    @Test
    void t500_join() {
        db
                .select(READER.NAME, BOOK.TITLE)
                .from(READER)
                .join(READER_BOOK).on(READER.ID.eq(READER_BOOK.READER_ID))
                .join(BOOK).on(BOOK.ID.eq(READER_BOOK.BOOK_ID))
                .fetch();
    }

    @Test
    void t500_joinMultiple() {
        db
                .selectDistinct(
                        READER.NAME,
                        BOOK.TITLE,
                        count(AUTHOR.NAME).over().partitionBy(READER.NAME)
                )
//                .selectDistinct(READER.NAME, countDistinct(AUTHOR.NAME))
                .from(READER)
                .leftJoin(READER_BOOK).on(READER.ID.eq(READER_BOOK.READER_ID))
                .leftJoin(BOOK).on(BOOK.ID.eq(READER_BOOK.BOOK_ID))
                .leftJoin(AUTHOR_BOOK).on(AUTHOR_BOOK.BOOK_ID.eq(BOOK.ID))
                .leftJoin(AUTHOR).on(AUTHOR.ID.eq(AUTHOR_BOOK.AUTHOR_ID))
//                .groupBy(READER.NAME)
                .orderBy(1)
                .fetch();
    }

    @Test
    void t600_joinSimplified() {
        db
                .select(READER_BOOK.reader().NAME, READER_BOOK.book().TITLE)
                .from(READER_BOOK)
                .fetch();
    }

    @Test
    void t610_multiset() {
        record Reader(String name) {}
        record Author(String name, Integer birthYear) {}
        record Book(String name, List<Reader> readers, List<Author> authors) {}

        db
                .select(
                        BOOK.TITLE,
                        multiset(
                                select(READER_BOOK.reader().NAME)
                                .from(READER_BOOK)
                                .where(READER_BOOK.BOOK_ID.eq(BOOK.ID))
                        ).convertFrom(r -> r.map(mapping(Reader::new))),
                        multiset(
                                select(AUTHOR_BOOK.author().NAME, AUTHOR_BOOK.author().BIRTH_YEAR)
                                .from(AUTHOR_BOOK)
                                .where(AUTHOR_BOOK.BOOK_ID.eq(BOOK.ID))
                        ).convertFrom(r -> r.map(mapping(Author::new)))
                )
                .from(BOOK)
                .fetch(mapping(Book::new))
                .forEach(book -> System.out.printf("%s read %s by %s%n", book.readers(), book.name, book.authors()));
    }

    @Test
    void t600_union() {
        db
                .select(READER_BOOK.reader().NAME, READER_BOOK.book().TITLE)
                .from(READER_BOOK)
                .union(
                        select(AUTHOR_BOOK.author().NAME, AUTHOR_BOOK.book().TITLE)
                        .from(AUTHOR_BOOK)
                )
                .fetch();
    }

    @Test
    void t700_subselect() {
        Reader r = READER.as("r");
        ReaderBook rb = READER_BOOK.as("rb");
        db
                .select(r.NAME)
                .from(r)
                .where(r.ID.in(
                        select(rb.READER_ID)
                        .from(rb)
                        .where(rb.book().as("b").TITLE.like("English%"))
                ))
                .fetch();
    }

    @Test
    void t800_insert() {
//        var reader = db.newRecord(READER);
//        reader.setId(5L);
//        reader.setName("Hulk");
//        reader.store();

        db.insertInto(READER)
                .columns(READER.ID, READER.NAME)
                .values(6L, "Robin")
                .execute();
    }

    @Test
    void t900_update() {
//        var hulk = db.selectFrom(READER)
//                .where(READER.NAME.eq("Hulk"))
//                .fetchOne();
//        hulk.setName("Joker");
//        hulk.store();

        db.update(READER)
                .set(READER.NAME, "Hulk")
                .where(READER.NAME.eq("Joker"))
                .execute();
    }


}
