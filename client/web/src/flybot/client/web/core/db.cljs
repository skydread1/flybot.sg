(ns flybot.client.web.core.db
  "State management using re-frame.
   
   ## Naming convention (inspired by Ivan Fedorov)
   :evt.domain/evt-id for events
   :subs.domain/sub-id for subs
   :domain/key-id for db keys
   :fx.domain/fx-id for effects
   :cofx.domain/cofx-id for coeffects"
  (:require [flybot.client.web.core.db.event]
            [flybot.client.web.core.db.fx]
            [flybot.client.web.core.db.sub]))

