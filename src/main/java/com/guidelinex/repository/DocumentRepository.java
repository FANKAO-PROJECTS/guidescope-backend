package com.guidelinex.repository;

import com.guidelinex.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

  @Query(value = """
      SELECT
        id,
        type,
        title,
        year,
        link,
        keywords,
        slug,
        created_at,
        ts_rank(
          setweight(to_tsvector('english', title), 'A') ||
          setweight(
            to_tsvector(
              'english',
              coalesce(array_to_string(keywords, ' '), '')
            ),
            'B'
          ),
          websearch_to_tsquery('english', :query)
        ) AS score,
        COUNT(*) OVER() AS total_count
      FROM documents
      WHERE
        (CAST(:query AS text) IS NULL OR :query = '' OR search_vector @@ websearch_to_tsquery('english', :query))
        AND (CAST(:types AS text[]) IS NULL OR type = ANY(CAST(:types AS text[])))
        AND (CAST(:year_from AS integer) IS NULL OR year >= CAST(:year_from AS integer))
        AND (CAST(:year_to AS integer) IS NULL OR year <= CAST(:year_to AS integer))
      ORDER BY
        CASE WHEN (CAST(:query AS text) IS NULL OR :query = '') THEN 0 ELSE 1 END DESC,
        score DESC,
        year DESC
      LIMIT :limit OFFSET :offset
      """, nativeQuery = true)
  List<Object[]> searchDocuments(
      @Param("query") String query,
      @Param("types") String[] types,
      @Param("year_from") Integer yearFrom,
      @Param("year_to") Integer yearTo,
      @Param("limit") int limit,
      @Param("offset") int offset);
}
