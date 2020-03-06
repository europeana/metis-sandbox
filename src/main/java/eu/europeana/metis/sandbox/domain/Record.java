package eu.europeana.metis.sandbox.domain;

import eu.europeana.metis.sandbox.common.locale.Country;
import eu.europeana.metis.sandbox.common.locale.Language;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@Builder
@EqualsAndHashCode
@ToString
public class Record {

  @NonNull
  private final String recordId;
  @NonNull
  private final String datasetId;
  @NonNull
  private final String datasetName;
  @NonNull
  private final Country country;
  @NonNull
  private final Language language;
  @NonNull
  @EqualsAndHashCode.Exclude
  private final String content;

  public static Record from(Record record, String content) {
    return Record.builder()
        .recordId(record.getRecordId())
        .datasetId(record.getDatasetId())
        .datasetName(record.getDatasetName())
        .content(content)
        .country(record.getCountry())
        .language(record.getLanguage())
        .build();
  }
}
