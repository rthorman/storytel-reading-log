# CSV backup and restore

## Back up

Choose **Settings → Export reading data**. Save the `.csv` file in Downloads, Documents or another location outside the app. The backup contains all books, authors, series, formats, start/finish dates and book/series comments with their timestamps.

Keep a copy on another device or storage location. Backups contain your reading history in readable text; share them deliberately.

## Restore or move phones

1. Transfer the CSV using Quick Share, USB or a file service.
2. Install the app on the destination phone.
3. Choose **Settings → Import reading data** and select the file.
4. Review the counts of books, series and comments.
5. Confirm replacement of the destination’s current reading data. Export its existing data first if needed.

**Restore replaces the entire library; it does not merge libraries.** Validation happens before the confirmation. The database replacement is transactional: failure rolls back, retaining the prior data. Notification-access settings and debug history are not restored; enable access separately. Pause Storytel while restoring so a new notification does not add another entry immediately afterward.

## CSV structure

UTF-8, comma-separated, double-quoted fields, CRLF record endings. Quotes are doubled inside fields; commas, Unicode and multiline comments are supported. The first row is:

```csv
record_type,id,parent_id,title,author,series,format,started,finished,comment,created_utc
```

The first data record is `meta` with ID `1`, identifying format version 1. Other record types:

| Type | Meaning |
| --- | --- |
| `series` | Series ID and name in `series` |
| `book` | Book ID and metadata; `parent_id` links to a series when assigned |
| `book_comment` | Comment ID, book ID in `parent_id`, text and timestamp |
| `series_comment` | Comment ID, series ID in `parent_id`, text and timestamp |

Book dates use `YYYY-MM-DD`; comment timestamps use UTC `YYYY-MM-DD HH:mm`. IDs preserve relationships inside the file. An empty library still exports the header and format-version record.

Import rejects unsupported versions/types, wrong columns, invalid dates, duplicate IDs, missing parents, empty required values and malformed CSV. Limits: 25 MiB file size, 100,000 CSV records and 64 KiB characters per field. Empty date fields represent books not yet started or finished. Format is `Audiobook` or `Ebook`.

You can inspect a backup in a text editor or spreadsheet, but preserve its exact columns, metadata row, relationships and quoting if editing it. Spreadsheet programs may reinterpret dates or formula-like book/comment text; open untrusted CSV as text. A diagnostic JSONL export is not accepted as a reading-data backup.
