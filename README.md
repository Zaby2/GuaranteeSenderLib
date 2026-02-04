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

Параметры можно заполнить, используя `@ConfigurationProperties`. Например:
<img width="758" height="92" alt="image" src="https://github.com/user-attachments/assets/9b968cdb-178e-4458-a2cc-9dd5cf15cff1" />

## Инструкция по конфигурации

1. Первым шагом необходимо создать указанные выше классы, с учетом выбранного буфера(Параметр guarantee."---bufferType---".enabled) и обязательно `HttpSenderConfiguration`.
Назначение параметров можно посмотреть в таблице выше.
Пример:
<img width="942" height="180" alt="image" src="https://github.com/user-attachments/assets/3720b367-c3fe-4ed2-84b9-337ad61ed859" />

2. Создаются классы, отвечающие за отправку по целевому транспорту:
   - `HttpSender` — создается вручную
   - `KafkaSender` — можно получить как бин контекста
   - `MongoDbSender` — можно получить как бин контекста
   - `SqlSender` — можно получить как бин контекста

3. Для каждого `sender` необходимо создать `BalancingProvider`, установить имя, вес.
   
  - Пример:
    
  <img width="756" height="344" alt="image" src="https://github.com/user-attachments/assets/b37d6bc3-1ed9-41f8-af61-0a45145cf3e5" />
  
  - Важно отметить, что атрибут веса для http указывается на уровне `BalancingProvider` и используется для балансировки нагрузки между хостами.
    
4. Классы `BalancingProvider` объединяются в `BalancingGroupConfiguration` по типу отправки(Http, Broker, Sql, NoSql):
   
  Таким образом, структура выглядит:
  
  <img width="981" height="475" alt="image" src="https://github.com/user-attachments/assets/d874cd4f-d22b-4560-a416-65230f192252" />
  
 - У `BalancingGroupConfiguration` также можно установить атрибуты: имя, обязательно установить атрибут типа (в соответствии с транспортом) и указать вес.
   
- Важно отметить, что атрибут веса для буферов (`Broker`, `Sql`, `NoSql`) указывается на уровне `BalancingGroupConfiguration` и используется для выставления приоритета временного хранилища (буфера).  
  Например, если у SQL-группы вес меньше, чем у NoSQL-группы, то временным хранилищем будет выступать NoSQL-группа, а в случае её недоступности основным буфером станет SQL-группа.
  
  Пример:
  
  <img width="523" height="180" alt="image" src="https://github.com/user-attachments/assets/35316b74-1369-4a09-9d29-20ac23466e2f" />
  
5. Необходимо создать сервис для подписания ЭЦП - `SignatureService`, передав в конструктор либо `JksKeyStoreProvider`, либо `PemKeyStoreProvider` в зависимости от типа используемых сертификатов `JKS` или `PEM` соответсвенно.
   
  Пример:
  
  <img width="565" height="161" alt="image" src="https://github.com/user-attachments/assets/b0b822c4-7f56-49e3-a523-d5c84a0bd785" />
  
6. Следующим шагом создается `GuaranteeSenderConfiguration`, который содержит в себе все `BalancingGroupConfiguration` списком и `SignatureService`. Также на этом уровне задается конфигурация CircuitBreaker - `CircuitBreakerConfiguration`, в параметры принмиает кол-во ошибок, до переходав `OPEN` и параметр времени перехода в `HALF_OPEN`. 

Пример:

<img width="736" height="199" alt="image" src="https://github.com/user-attachments/assets/2e5178c2-fd0e-49e0-8b7e-8112ca1ba307" />

7. Создается прокси - `GuaranteeSenderProxyImpl<?>`, параметризируется типом, который ожидается по контракту с http хостом. Принимает `GuaranteeSenderConfiguration`, `GuaranteeMonitoring`, `класс параметра`.
   
   - `GuaranteeMonitoring` - можно получить как бин контекста.
     
  Пример:
  <img width="836" height="172" alt="image" src="https://github.com/user-attachments/assets/3866a6ab-a500-48dc-928b-972b6343be45" />

## Использование 

Для использования достаточно в точке отправки клиентского приложения получить бин `GuaranteeSenderProxyImpl<?>` и вызвать метод `send`, передав запрос, которым параметризирован прокси. 

Пример:

<img width="598" height="250" alt="image" src="https://github.com/user-attachments/assets/ee8a61e8-0746-4720-8e99-94d5eeea5ee7" />

## WorkFlow

1. `GuaranteeSenderProxyImpl<?>` - разделяет список `BalancingGroupConfiguration` на HTTP - main и остальные группы.
   
2. Вызывается асинхронный метод `send`.
   
   2.1 Полученный запрос конвертируется во внутреннее DTO библиотеки - `GuaranteeSenderDto`
   
   2.2 Происходит попытка отправки в `http` группу `BalancingGroupConfiguration`. Из нее выбирается `BalancingProvider`(с использованием балансировщика по весу - `WeightedLoadBalancer`), если он доступен в  `CircuitBreaker`, то происходит попытка отправки через `HttpSender`, иначе выбирается следующий `BalancingProvider`. При отправке в через  `HttpSender` происходит десериализация данных из `GuaranteeSenderDto` в тип, которым параметризирован соответсвующий  `GuaranteeSenderProxyImpl<?>`.
   
   2.3 В случае недоступности всех `BalancingProvider` в в `http` группе переход к шагу 3, иначе отправка считается успешной.
   
3. Вызывается метод `sendToBuffer`
   3.1 Данные подписываются ЭЦП в `SignatureService`
   3.2 Начинается итерация по оставшимся `BalancingGroupConfiguration`(по указанному в группе весу), происходит итерация по  `BalancingProvider`, просходит попытка отправки. В случае успешной отправки процесс считается завершенным(переход в ## Puller workflow), иначе процеес считается ошибочным, отбрасывается критичиская метрика(проблема связана с инфраструктурой).

## Puller workFlow
Puller-классы отвечают за получение ранее сохраненных данных из буфера(п 3.2 EorkFlow) и их повторную доотправку. 
Классы конфигурации `pulle`r: 

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

  




   





