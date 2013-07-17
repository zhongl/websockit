**websockit** provide stub and driver for testing WebSocket applicaiton.

## Install

### Install

- install [SBT](http://www.scala-sbt.org/release/docs/Getting-Started/Setup.html)

- clone [websocket-kit](https://github.com/zhongl/websocket-kit)

```
> git clone git@github.com:zhongl/websockit.git
```

## Usage

- create a server config sample

```
> cd websockit
> sbt run
```

- edit config sample for your needs, eg: An echo server like,

```scala
import zhongl.websockit.dsl._
import zhongl.websockit.stub._

new WebSocketServer(port = 12306, path = "/ws") {
  def receive = {
     case f => Some(f.retain)
  }
}
```

- start server

```
> sbt run server
```


## Real Config

```scala
import zhongl.websockit.dsl._
import zhongl.websockit.stub._

new WebSocketServer(port = 12306, path = "/ws") {
  def receive = {
    case Text(json) if json ? "$.to" == "allen" => Text(s"""{"code":200, "uuid":${json ? "$.uuid"}}""")
  }
}
```

run the config above, then send json ` {"to":"allen", "uuid":1}` to `ws://localhost:12306/ws`, you should get `{"code":200, "uuid":1}`

### Tips

- use [JSONPath](http://goessner.net/articles/JsonPath/) like `$.to`, for querying json value
- use `s"""${var}"""` format text with variables
- without head `s` a raw text/json like `"""[1,2,3]"""` would be return


## Copyright and license

Copyright 2013 zhongl

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
