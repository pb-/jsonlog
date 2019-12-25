# JSON log

JSON log is a simple commit log of json objects with an HTTP interface. This document describes the JSON log API; a minimum-viable-product [implementation](mvp) is available.


## Interface overview

The interface supports the following operations.

 * Append a sequence of JSON objects (possibly just one) to the log.
 * Retrieve a slice of JSON objects from the log (possibly a slice without bounds, meaning the entire log.)


## Storing data

You can store lines of JSON objects to a given log through HTTP post. The trailing newline of the last JSON Object posted may be omitted.

```bash
curl -X POST https://example.com/log-123 \
  -H 'Content-Type: text/plain' \
  -d '{"message": "hello world"}'
```
```json
{"offset": 0}
```


## Retrieving data

You can retrieve JSON lines from the log through HTTP get.

```bash
# retrieve the entire log
curl https://example.com/log123
```
```json
{"message": "hello world"}
{"message": "how are you?"}
```

A slice of offsets can be selected with `from` (inclusive).

```bash
# skip over the first object in the log
curl https://example.com/log123?from=1
```
```json
{"message": "how are you?"}
```

A negative value for `from` indicates the nth item from the end of the log.

```bash
# retrieve the last item in the log
curl https://example.com/log123?from=-1
```
```json
{"message": "how are you?"}
```

Note that slices can be empty.

Retrieval responses with at least one JSON object include a header value `Jsonlog-Offset` indicating the (positive) offset of the first value returned.


## Advanced usage

When storing data, you can optionally specify a paramter `offset` to assert that you expect this offset for the first item in the request. An error (HTTP 400) is returned and nothing is stored if this expectation is not met.

```bash
curl -X POST https://example.com/log-123?offset=1 \
  -H 'Content-Type: text/plain' \
  -d '{"message": "how is life?"}'
```
```json
{"code": "offset-mismatch", "detail": "The expected offset does not match the actual offset."}
```

The primary use cases for this feature are supporting idempotency (attempting to replay a request with `offset` will fail) and consistency guarantees in the presence of concurrency (making sure that nothing was added between the last known offset and the new object).
