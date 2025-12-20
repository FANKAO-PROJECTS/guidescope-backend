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
          plainto_tsquery('english', :query)
        ) AS score
      FROM documents
      WHERE
        search_vector @@ plainto_tsquery('english', :query)
        AND (CAST(:types AS text[]) IS NULL OR type = ANY(CAST(:types AS text[])))
        AND (CAST(:year_from AS integer) IS NULL OR year >= :year_from)
        AND (CAST(:year_to AS integer) IS NULL OR year <= :year_to)
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
