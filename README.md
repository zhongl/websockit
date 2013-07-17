**WebSocketKit** provide stub and driver for testing WebSocket applicaiton.

## Install

- install [Homebrew](http://mxcl.github.io/homebrew/)

```
> ruby -e "$(curl -fsSL https://raw.github.com/mxcl/homebrew/go)"
```

- install [SBT](http://www.scala-sbt.org)

```
> brew install sbt
```

- clone [websocket-kit](https://github.com/zhongl/websocket-kit)

```
> git clone git@github.com:zhongl/websocket-kit.git
```

## Usage

- create a server config sample

```
> cd websocket-kit
> sbt run
```

- edit config sample for your needs, eg: An echo server like,

```scala
import zhongl.websocketkit.dsl._
import zhongl.websocketkit.stub._

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
