up:
	docker-compose up -d --build
.PHONY: up

build:
	docker build . -t jsonlog
.PHONY: build

lint:
	clj -A:eastwood
.PHONY: lint

devrepl:
	MYSQL_HOST=127.0.0.1 \
	MYSQL_PORT=13306 \
	MYSQL_DATABASE=jsonlog \
	MYSQL_USER=jsonlog \
	MYSQL_PASSWORD=jsonlog \
	clj -R:nrepl -m nrepl.cmdline
.PHONY: devrepl
