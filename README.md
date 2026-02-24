# GuaranteeSenderLib

## Описание

Библиотека предоставляет proxy-клиент, позволяющий минимизировать потери данных при Rest взаимодействии между двумя сервисами. Для промежуточного хранения данных предоставляется 3 типа буфера: SQL базы, NOSQL (MongoDb), Брокеры (Kafka). Очереди можно совмещать и комбинировать между собой по необходимости.

## Структура проекта

Проект состоит из 4-х основных модулей:

- **configuration-core** — общий модуль конфигурации. Здесь задается структура отправки через Rest и в очереди, здесь же находятся вспомогательные конфигурации для корректной работы.
- **pull-processing-core** — модуль, отвечающий за обработку сообщений в буфере, а также их очистку, если последняя не предусмотрена буфером.
- **signature-core** — модуль создания/проверки ЭЦП на основе pem/jks сертификатов.
- **guarantee-proxy** — модуль, являющийся входной точкой для клиента. В нем расположена логика балансировки нагрузки и circuitBreaker для Rest, а также общая логика взаимодействия и переключения между транспортами.

Также представлены два модуля `ClientExample` и `ServerExample` для демонстрации работы библиотеки.

### configuration-core

<img width="694" height="641" alt="image" src="https://github.com/user-attachments/assets/cf55caac-73e8-44b9-b06b-4a435c1341b4" />

`GuaranteeSenderConfiguration` — общая конфигурация для работы в модуле **guarantee-proxy**. Включает в себя список логических групп балансировки (`BalancingGroupConfiguration`), в исходной реализации это SQL, NOSQL и Broker группы.

Каждая `BalancingGroupConfiguration` включает в себя список провайдеров `BalancingProvider`, содержащих все необходимое для отправки в конкретную группу.

Для REST взаимодействий реализована балансировка нагрузки внутри провайдеров, с использованием указанного атрибута веса провайдера (`BalancingProvider.weight`). Также для REST взаимодействий реализованы CIRCUIT BREAKER и RETRY CONFIG.

<img width="847" height="654" alt="image" src="https://github.com/user-attachments/assets/7d2f1d6f-ca8a-40a1-8da9-d36f7523776e" />

Важным моментом является то, что на каждый хост REST взаимодействия используется свой `BalancingProvider`, т.к. требования по нагрузке, требования к повторным запросам могут варьироваться от сервера к серверу.

Для логического объединения буферов создавать несколько `BalancingProvider` не требуется, т.к. в библиотеке предусмотрена возможность использовать любое количество (и любую реализацию SQL БД), любое количество MongoDB.

Для KAFKA эта проблема не является актуальной, т.к. её единицей масштабирования являются кафка-брокеры.
Каждый BalancingProvider включает в себя реализацию интерфейса GuaranteeSender, который занимается отправкой по целевому транспорту. 

### pull-processing-core

Главной единицей взаимодействия в этом модуле является интерфейс `PullProcessing`, реализации которого предоставляют возможность повторного получения данных из буферов.

Все реализации работают в соответствии с расписанием, задаваемым клиентом с использованием CRON.

После получения неотправленных сообщений проводится проверка ЭЦП, в случае, если она не пройдена, сообщение повторно отправлено не будет.

По прохождении проверки, сообщение передается в `guarantee-proxy`, где повторно проходит попытку отправки.

<img width="507" height="362" alt="image" src="https://github.com/user-attachments/assets/f8c9473e-e7ed-4f2b-aa83-c941a603ffcd" />

Интерфейс `CleanProcessor` служит для очистки устаревших, уже отправленных данных. Реализации представлены для SQL, MongoDB. В Kafka очистка настраивается на стороне сервера посредством RETENTION POLICY.

<img width="635" height="357" alt="image" src="https://github.com/user-attachments/assets/5b772295-6e30-4054-a8b1-e8029561bb0a" />

`SchedulerConfiguration` сконфигурирована таким образом, что под процессы по расписанию выделяется отдельный пул потоков. Это позволяет сократить используемые приложением ресурсы.

### signature-core

<img width="545" height="291" alt="image" src="https://github.com/user-attachments/assets/3804e4b2-4002-43e6-9d90-b2ae98425d99" />

Модуль предоставляет возможность подписи отправляемых данных ЭЦП с использованием PEM / JKS сертификатов. В проекте предоставлен пример PEM сертификатов, сгенерированных командой:

openssl req -x509 \
  -newkey rsa:2048 \
  -keyout key.pem \
  -out cert.pem \
  -days 365 \
  -nodes \
  -subj "/CN=localhost" \
  -addext "subjectAltName = DNS:localhost,IP:127.0.0.1"

### guarantee-proxy 

Основной модуль — является точкой входа при попытке отправки данных.

При получении `GuaranteeSenderConfiguration` группы разбиваются на главную и буфер-группы. Главной группой по умолчанию является HTTP группа, остальные считаются буферами.

В цикле происходит балансировка нагрузки между `BalancingProvider` HTTP группы. В случае успешной отправки процесс заканчивается, иначе переходим к отправке в запасные буфер-группы.

Перед отправкой в БД или брокер данные обязательно подписываются ЭЦП, затем переходят к отправке в буфер, аналогично используя `BalancingProvider.sender`.

<img width="664" height="701" alt="image" src="https://github.com/user-attachments/assets/d812c963-be18-412f-b3d1-519551046e54" />

После вызова `GuaranteeSenderProxy.send()` все действия происходят в отдельном потоке. Для экономии ресурсов и оптимизации под отправку данных выделяется пул виртуальных потоков.

### Структура данных 

`GuaranteeSenderDto` — является единой сущностью, используемой внутри библиотеки для передачи и хранения данных. В случае с буфером структура таблиц полностью соответствует структуре этого ДТО, для REST взаимодействий данные десериализуются в сущность, которой параметризирован `GuaranteeSenderProxy<T>`.

<img width="465" height="192" alt="image" src="https://github.com/user-attachments/assets/8823f7a5-8340-472e-b4a1-9b089ace0f17" />

### Нефункциональные требования

В качестве логирования используется SLF4J, логируются все критически важные события, происходящие с данными, что позволяет легко отладить/просмотреть трассировку процесса.

Также в проекте используется ряд метрик, для настройки дашбордов в Grafana и Alerts. Отвечает за это пакет `monitoring` в модуле `configuration-core`.

По каждой метрике разделяются `FAIL` / `SUCCESS` события:

- **HTTP_SENDER** — метрика указывает на REST взаимодействие.
- **SQL_SENDER** — метрика указывает на SQL взаимодействие.
- **NO_SQL_SENDER** — метрика указывает на NO_SQL взаимодействие.
- **BROKER_SENDER** — метрика указывает на BROKER взаимодействие.
- **SQL_PULLER** — метрика указывает на операцию вычитки из SQL.
- **NO_SQL_PULLER** — метрика указывает на операцию вычитки из NO-SQL.
- **BROKER_PULLER** — метрика указывает на операцию вычитки из BROKER.
- **SQL_CLEANER** — метрика указывает на операцию очистки SQL БД.
- **NO_SQL_CLEANER** — метрика указывает на операцию очистки NO-SQL БД.
- **SIGN_DATA** — метрика указывает на операцию подписи данных (является критической).
- **SIGNATURE_CHECK** — метрика указывает на операцию проверки подписи данных (является критической).
- **GUARANTEE_SENDER** — метрика, отображающая FAIL операцию по всему процессу взаимодействия с гарантированной доставкой (цель: отобразить, что не удалось отправить ни в один буфер и данные потеряны, является критической).
- **TRANSPORT** — общая метрика, отображающая количество FAIL операций по транспортам.

### Пример конфигурации

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  metrics:
    tags:
      application: my-app
guarantee:
  nosql:
    enabled: false
    mongo:
      sender:
        connections:
          db1: mongodb://user:1234@localhost:27017/?authSource=admin
      puller:
        limit: 5
        cron: "0 15 10 * * ?"
      cleaner:
        limit: 5
        cron: "*/15 * * * * ?"
  sql:
    enabled: true
    sender:
      properties-map:
        db1:
          url: jdbc:postgresql://localhost:5432/db1
          user-name: user
          password: 1234
          driver-class-name: org.postgresql.Driver
    puller:
      limit: 10
      cron: "*/35 * * * * ?"
    cleaner:
      limit: 5
      cron: "0 15 10 * * ?"
  kafka:
    enabled: true
    sender:
      topic: test-topic
      bootstrap-servers: localhost:9092
      client-id: main-producer
      acks: 0
      max-retries: 1
      rq-timeout-ms: 10000
      delivery-timeout-ms: 10000
    puller:
      durationSec: "10"
      cron: "*/10 * * * * ?"
      topic: test-topic
      bootstrap-servers: localhost:9092
      group-id: main-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      max-poll-records: 100
  http1:
    url: http://localhost:8081/test
    headersMap:
      Accept: "application/json"
    retryConfiguration:
      exceptionsMap:
        org.springframework.web.client.HttpServerErrorException: true
      maxAttempts: 3
      initialInterval: 5
      intervalMultiplier: 5
      maxInterval: 100
  http2:
    url: http://localhost:8081/test/2
    headersMap:
      Accept: "application/json"
    retryConfiguration:
      exceptionsMap:
        org.springframework.web.client.HttpServerErrorException: true
      maxAttempts: 3
      initialInterval: 5
      intervalMultiplier: 5
      maxInterval: 100
```
| Название параметра | Описание | Обязательность |
| ------- | ------- | ------- |
| management.endpoints.web.exposure.include | Список веб-эндпоинтов для мониторинга (health, info, prometheus) | Обязательный |
| management.metrics.tags.application | Название приложения для метрик | Обязательный |
| guarantee.nosql.enabled | Параметр подключает использование NoSql буфера | Необязательный(по умолчанию false) |
| guarantee.nosql.mongo.sender.connections.db1 | Строка подключения к NoSQL буферу | Условно обязательный (если nosql.enabled=true и используется mongo sender) |
| guarantee.nosql.mongo.puller.limit | Лимит записей для выгрузки из Mongo | Условно обязательный (если nosql.enabled=true и используется mongo sender) |
| guarantee.nosql.mongo.puller.cron | CRON выражение задает расписание выгрузки записей из Mongo буфера | Условно обязательный (если nosql.enabled=true и используется mongo sender) |
| guarantee.nosql.mongo.cleaner.limit | Кол-во записей, очищаемых в Mongo за раз | Условно обязательный (если nosql.enabled=true и используется mongo sender) |
| guarantee.nosql.mongo.cleaner.cron | CRON выражение для запуска очистки bp Mongo | Условно обязательный (если nosql.enabled=true и используется mongo sender) |
| guarantee.sql.enabled | Параметр подключает использование Sql буфера | Необязательный(по умолчанию false) |
| guarantee.sql.sender.properties-map.db1.url | URL подключения к БД SQL | Условно обязательный (если sql.enabled=true и используется sql sender) |
| guarantee.sql.sender.properties-map.db1.user-name | Имя пользователя для подключения к БД | Условно обязательный (если sql.enabled=true и используется sql sender) |
| guarantee.sql.sender.properties-map.db1.password | Пароль для подключения к БД | Условно обязательный (если sql.enabled=true и используется sql sender) |
| guarantee.sql.sender.properties-map.db1.driver-class-name | Класс JDBC драйвера | Условно обязательный (если sql.enabled=true и используется sql sender) |
| guarantee.sql.puller.limit | Лимит записей для выгрузки из SQL БД | Условно обязательный (если sql.enabled=true и используется sql sender) |
| guarantee.sql.puller.cron | CRON выражение для запуска выгрузки из SQL БД | Условно обязательный (если sql.enabled=true и используется sql sender) |
| guarantee.sql.cleaner.limit | Лимит записей для очистки из SQL БД | Условно обязательный (если sql.enabled=true и используется sql sender) |
| guarantee.sql.cleaner.cron | CRON выражение для запуска очистки SQL БД | Условно обязательный (если sql.enabled=true и используется sql sender) |
| guarantee.kafka.enabled | Включение/выключение Kafka компонентов | Необязательный(по умолчанию false) |
| guarantee.kafka.sender.topic | Название топика Kafka для отправки сообщений | Условно обязательный (если kafka.enabled=true и используется kafka sender) |
| guarantee.kafka.sender.bootstrap-servers | Адрес серверов Kafka | Условно обязательный (если kafka.enabled=true и используется kafka sender) |
| guarantee.kafka.sender.client-id | ID клиента Kafka продюсера | Условно обязательный (если kafka.enabled=true и используется kafka sender) |
| guarantee.kafka.sender.acks | Уровень подтверждения получения сообщений | Условно обязательный (если kafka.enabled=true и используется kafka sender) |
| guarantee.kafka.sender.max-retries | Максимальное количество попыток отправки сообщения | Условно обязательный (если kafka.enabled=true и используется kafka sender) |
| guarantee.kafka.sender.rq-timeout-ms | Таймаут запроса в миллисекундах | Условно обязательный (если kafka.enabled=true и используется kafka sender) |
| guarantee.kafka.sender.delivery-timeout-ms | Таймаут доставки сообщения в миллисекундах | Условно обязательный (если kafka.enabled=true и используется kafka sender) |
| guarantee.kafka.puller.durationSec | Продолжительность выгрузки сообщений в секундах | Условно обязательный (если kafka.enabled=true и используется kafka sender) |
| guarantee.kafka.puller.cron | CRON выражение для запуска выгрузки из Kafka | Условно обязательный (если kafka.enabled=true и используется kafka sender) |
| guarantee.kafka.puller.topic | Название топика Kafka для выгрузки сообщений | Условно обязательный (если kafka.enabled=true и используется kafka puller) |
| guarantee.kafka.puller.bootstrap-servers | Адрес серверов Kafka | Условно обязательный (если kafka.enabled=true и используется kafka puller) |
| guarantee.kafka.puller.group-id | ID группы Kafka консьюмеров | Условно обязательный (если kafka.enabled=true и используется kafka puller) |
| guarantee.kafka.puller.key-deserializer | Класс десериализатора ключа сообщения | Условно обязательный (если kafka.enabled=true и используется kafka puller) |
| guarantee.kafka.puller.value-deserializer | Класс десериализатора значения сообщения | Условно обязательный (если kafka.enabled=true и используется kafka puller) |
| guarantee.kafka.puller.auto-offset-reset | Поведение при отсутствии оффсета (earliest/latest) | Условно обязательный (если kafka.enabled=true и используется kafka puller) |
| guarantee.kafka.puller.enable-auto-commit | Включение/выключение автокоммита оффсетов | Условно обязательный (если kafka.enabled=true и используется kafka puller) |
| guarantee.kafka.puller.max-poll-records | Максимальное количество записей за один опрос | Условно обязательный (если kafka.enabled=true и используется kafka puller) |
| guarantee.http1.url | URL для HTTP запроса | Обязательный |
| guarantee.http1.headersMap | Заголовки HTTP запроса | Обязательный |
| guarantee.http1.retryConfiguration.exceptionsMap | Исключения, при которых производится повторный запрос | Обязательный |
| guarantee.http1.retryConfiguration.maxAttempts | Максимальное количество попыток запроса | Обязательный |
| guarantee.http1.retryConfiguration.initialInterval | Начальный интервал между попытками в секундах | Обязательный |
| guarantee.http1.retryConfiguration.intervalMultiplier | Множитель интервала между попытками | Обязательный |
| guarantee.http1.retryConfiguration.maxInterval | Максимальный интервал между попытками в секундах | Обязательный |


### Руководство прикладного разработчика 

## Необходимые для работы библиотеки DTO с параметрами:

- **HTTP**: `HttpSenderConfiguration`
- **SQL**: `SqlSenderProperties`
- **MONGO**: `MongoSenderDto`
- **KAFKA**: `KafkaSenderProperties`, `KafkaPullProcessorConfigDto`

## Использование 

Для использования достаточно в точке отправки клиентского приложения получить бин `GuaranteeSenderProxyImpl<?>` и вызвать метод `send`, передав запрос, которым параметризирован прокси. 

Пример:

<img width="598" height="250" alt="image" src="https://github.com/user-attachments/assets/ee8a61e8-0746-4720-8e99-94d5eeea5ee7" />

## WorkFlow

1. `GuaranteeSenderProxyImpl<?>` - разделяет список `BalancingGroupConfiguration` на HTTP - main и остальные группы(по полю Type).
   
2. Вызывается асинхронный метод `send`.
   
   2.1 Полученный запрос конвертируется во внутреннее DTO библиотеки - `GuaranteeSenderDto`
   
   2.2 Происходит попытка отправки в `http` группу `BalancingGroupConfiguration`. Из нее выбирается `BalancingProvider`(с использованием балансировщика по весу - `WeightedLoadBalancer`), если он доступен в  `CircuitBreaker`, то происходит попытка отправки через `HttpSender`, иначе выбирается следующий `BalancingProvider`. При отправке в через `HttpSender` происходит десериализация данных из `GuaranteeSenderDto` в тип, которым параметризирован соответсвующий  `GuaranteeSenderProxyImpl<?>`.
   
   2.3 В случае недоступности всех `BalancingProvider` в в `http` группе переход к шагу 3, иначе отправка считается успешной.
   
3. Вызывается метод `sendToBuffer`
   
   3.1 Данные подписываются ЭЦП в `SignatureService`
   
   3.2 Начинается итерация по оставшимся `BalancingGroupConfiguration`(по указанному в группе весу), происходит итерация по  `BalancingProvider`, просходит попытка отправки. В случае успешной отправки процесс считается завершенным(переход в ## Puller workflow), иначе процеес считается ошибочным, отбрасывается критичиская метрика(проблема связана с инфраструктурой).

   3.3 Запись сохраняетя в буфер:

   Пример:


| id | createdAt                  | isSent         | polledAt | requestType        | requestValue | signature                                                                                                                                                     
|  2 | 2026-02-05 10:58:54.467000 | 0x00           | NULL     | ru.bsh.dto.Request | {"id":1}     | <siganture> |

Здесь `requestType` - тип, в который будет десериализована запись при передаче в `HttpSender`. 
Сама запись имеет структуру `GuaranteeSenderDto`.


## Puller workFlow
Puller-классы отвечают за получение ранее сохраненных данных из буфера(п 3.2 WorkFlow) и их повторную доотправку. 
Классы конфигурации `puller`: 

- `SqlPullProcessorConfiguration`
- `NoSqlPullProcessorConfiguration`
- `KafkaPullProcessorConfig`

Классы реализации `puller`:

- `MongoPullProcessor`
- `SqlPullProcessor`
- `KafkaPullProcessor`
  
1. В соответствии с заданным расписанием `CRON ` выполняются запросы к буферу на получение записей.
   
2. Выполняется проверка ЭЦП
   
   2.1 Если проверка невалидна, то запись считаеся поврежденной, осуществялется переход к следующей полученной записи и шагу 4, иначе переход к шагу 3.
   
3. Вызывается метод `send` у `GuaranteeSenderProxyImpl<?>` -> переход к шагу 1. ## WorkFlow
   
4. Запись помечается в буфере как "прочитанная" (isSend = true)

## Cleaner workFlow
Cleaner-классы отвечают за удаление отправленных записей, имеющих пометку isSend = true в буфере. 

Классы реализации `cleaner`:

- `MongoDbCleanProcessor`
- `SqlCleanProcessor`
  
Для `kafka` реализаця не предусмотрена, тк очистка в топиках происходит по средствам `RETENTION_POLICY`

1. В соответствии с заданным расписанием `CRON ` выполняются запросы к буферу на удаление записей, имеющих пометку `isSend` = true.

 ### PATCH 
Цель обновления - упростить клиентское взаимодействие с библиотекой. Клиенту незачем знать о внутренних структурах библиотеки при использвоании стандартной реализации,
поэтмоу было принято решение созадть автоконфигурацию для проекта 

## Что нужно сделать теперь? 

1. Цель отправить данные серверу по `REST` -> конфигурируем точки отправки для `HTTP` взаимодействий:
Делается это исключительно средствами `yaml`

```yaml
  guarantee.http:
    groupName: BogdanHttp # Имя группы, задается для удобного отоброжения в логах
    configurations:
      http1: # Этот ключ используется для именования точек отправки в логах
        weight: 1 # вес используется для балансировки нагрузки, если указано несколько хостов, например в данном случае между http1 и http2
        url: http://localhost:8081/test
        headersMap:
          Accept: "application/json"
        retryConfiguration: # Тут задается конфигурация повтороных вызовов 
          exceptionsMap:
            org.springframework.web.client.HttpServerErrorException: true
          maxAttempts: 3
          initialInterval: 5
          intervalMultiplier: 5
          maxInterval: 100
      http2:
        weight: 2
        url: http://localhost:8081/test/2
        headersMap:
          Accept: "application/json"
        retryConfiguration:
          exceptionsMap:
            org.springframework.web.client.HttpServerErrorException: true
          maxAttempts: 3
          initialInterval: 5
          intervalMultiplier: 5
          maxInterval: 100
```

Таким образом, при добавлении данной конфигурации будет происходить попытка отправки в http1 и http2 до первой успешной

   
2. Определяемся с выбором буфера. По умолчанию доступны `SQL`, `MONGO`, `KAFKA`.

    a. Для испоользования `SQL` аналогично достаточно обычного yaml:

```yaml
  guarantee.sql:
    enabled: true # флаг включения(по умолчанию выключена)
    groupName: BogdanSql # Имя группы, задается для удобного отоброжения в логах
    weight: 5 # Вес группы используется для приоритезации все группы буферов
    sender: # В sender аналогично можно задать несколько точек отправки, в данном случае это db1 и db2
      properties-map: # Можно указать одну БД, указывать несколько только в случае необходимости
        db1:
          url: jdbc:postgresql://localhost:5432/db1
          user-name: user
          password: 1234
          driver-class-name: org.postgresql.Driver
        db2:
          url: jdbc:mysql://localhost:3306/db2
          user-name: user
          password: 1234
          driver-class-name: com.mysql.cj.jdbc.Driver
    puller: # Конфинурация повторной отправки, в фоне вычитываются данные из db1/db2 и пытаются доотправиться через http1/http2
      limit: 10 # Кол-во записей получаемых за один раз
      cron: "*/35 * * * * ?"
    cleaner: # Конфигурация для фонового удаления ранее отправленных данных
      limit: 5
      cron: "*/10 * * * * ?"
```

Таким образом, используя эту конфигурацию и конфигурацию из п.1 - В случае недостпуности `http1` и `http2` данные будут помещеные в `db1` или `db2`
А затем в соответствии с  `puller.cron` будет совершена повторная попытка отправки в `http1` или `http2`

    b. Для испоользования `MONGO` аналогично достаточно обычного yaml. Если мы не хотим/не имеем возможности использовать SQL, можно выбрать этот вариант
    
```yaml
guarantee:
  nosql:
    enabled: true
    mongo:
      groupName: BogdanNoSql
      weight: 10
      sender:
        connections: # Можно использовать как одно, так и несколько подклбчений
          db1: mongodb://user:1234@localhost:27017/?authSource=admin
          db2: mongodb://user:1234@localhost:27017/?authSource=admin
      puller:
        limit: 5
        cron: "*/30 * * * * ?"
      cleaner:
        limit: 5
        cron: "*/15 * * * * ?"
```
Аналогично SQL в этом случае при недоступности `http1` и `http2` данные будут помещеные в `db1` или `db2`

    с. Аналогично заполняется конфигурация для Kafka:
```yaml
guarantee.kafka:
    enabled: false
    groupName: BogdanKafka
    weight: 15
    sender:
      topic: test-topic
      bootstrap-servers: localhost:9092
      client-id: main-producer
      acks: 0
      max-retries: 1
      rq-timeout-ms: 10000
      delivery-timeout-ms: 10000
    puller:
      durationSec: "10"
      cron: "*/10 * * * * ?"
      topic: test-topic
      bootstrap-servers: localhost:9092
      group-id: main-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      max-poll-records: 100
```

3. Теперь, когда сконфигурирован главный транспорт(Http группа) и временный буфер/несколько буферов
Необходимо сделать импорт в свой класс конфигурации `@Import(GuaranteeAutoConfiguration.class)`

4. Далее заполним клиентскую конфигурацию
   
    a. Создаем сервси подписания данных ЭЦП аналогичным образом создается черерз `yaml`:
   
```yaml
guarantee:
  signature:
    pem: true # или jks: true 
    keyPath: "key.pem" # путь до серта и ключа
    certPath: "cert.pem"
```

 b. Создаем непосредственно прокси-бины и финальную конфигурацию к ним 
 ![Снимок экрана 2026-02-24 в 23.18.56.png](../../Desktop/%D0%A1%D0%BD%D0%B8%D0%BC%D0%BE%D0%BA%20%D1%8D%D0%BA%D1%80%D0%B0%D0%BD%D0%B0%202026-02-24%20%D0%B2%2023.18.56.png)

Здесь конфигурируется CircuitBreaker, для обеспечения отказоустойчивости, а также передается список всех точек взаимодействий определенных в yaml(все Http, Bufer группы)

Далее можно пользоваться `GuaranteeSenderProxyImpl`, данные не потеряются!

Библиотека поддерживает создание нескольких `GuaranteeSenderProxyImpl`, однако конфигурация групп для них(в случае если есть отличия) остается клиенту.
