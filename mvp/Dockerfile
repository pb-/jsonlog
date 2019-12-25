FROM clojure:tools-deps

RUN mkdir /app
WORKDIR /app

COPY deps.edn /app
RUN echo "(println \"deps are up to date\")" | clj -

COPY resources /app/resources
COPY src /app/src

CMD ["clj", "-m", "jsonlog"]
