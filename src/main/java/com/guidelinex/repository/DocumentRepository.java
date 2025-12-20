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
        ) AS score
      FROM documents
      WHERE
        (:query IS NULL OR :query = '' OR search_vector @@ websearch_to_tsquery('english', :query))
        AND (CAST(:types AS text[]) IS NULL OR type = ANY(CAST(:types AS text[])))
        AND (:year_from IS NULL OR year >= CAST(:year_from AS integer))
        AND (:year_to IS NULL OR year <= CAST(:year_to AS integer))
      ORDER BY score DESC
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
