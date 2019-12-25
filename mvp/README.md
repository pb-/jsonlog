# jsonlog MVP implementation

An MVP implementation for the [JSON log API](../README.md), written in Clojure.

The storage backend uses MySQL because of its small footprint and because the requirements for the database are laughable. A great fit.


## Deployment

Build the Docker image with `make build` and set the `MYSQL_*` environment variables (check out `docker-compose.yml` to see which ones) when running the container.


## Development

Using `docker-compose` is the fastest way to get started.

```bash
docker-compose up -d --build
# or simply: make up
```

The API is reachable at http://localhost:5000.

Note that this may take a while (~30s) for a completely fresh run since the MySQL container needs quite some time to complete initilization.

### REPL-based development

To get a shorter feedback loop, you can also start an nREPL and use your editor.

```bash
# start the database first
docker-compose up -d db
# start a headless nrepl
make devrepl
```

In your editor, evaluate the marked comment at the end of `jsonlog.clj` to run the initial database setup and start the webserver (note that this one listens on port 3000, not 5000). The server will silently auto reload after any change.
