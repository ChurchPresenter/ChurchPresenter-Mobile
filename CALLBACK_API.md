# Callback APIs — Project Song & Add to Schedule

Both endpoints accept a `ScheduleItem` wrapped in a request body.  
All requests must include `X-Api-Key: <key>` header (or `?apiKey=<key>`) if API key is enabled.

---

## POST /api/project

Immediately sends the item to the presenter window (**goes live**).

### Song body

```json
{
  "item": {
    "type": "org.churchpresenter.app.churchpresenter.models.ScheduleItem.SongItem",
    "id": "any-string",
    "songNumber": 42,
    "title": "Amazing Grace",
    "songbook": "Hymns",
    "displayText": "42 - Amazing Grace"
  }
}
```

---

## POST /api/schedule/add

Adds the item to the schedule list (**does not go live**).

### Song body

Same body shape as `/api/project` above.

---

## Responses

**Success**
```json
{ "ok": true }
```

**Error**
```json
{ "error": "invalid request body" }
```

---

## WebSocket equivalents

Connect to `wss://<host>:8765/ws` and send:

### Project song
```json
{
  "type": "project",
  "payload": "{\"item\":{\"type\":\"org.churchpresenter.app.churchpresenter.models.ScheduleItem.SongItem\",\"id\":\"1\",\"songNumber\":42,\"title\":\"Amazing Grace\",\"songbook\":\"Hymns\",\"displayText\":\"42 - Amazing Grace\"}}"
}
```

### Add to schedule
```json
{
  "type": "add_to_schedule",
  "payload": "{\"item\":{\"type\":\"org.churchpresenter.app.churchpresenter.models.ScheduleItem.SongItem\",\"id\":\"1\",\"songNumber\":42,\"title\":\"Amazing Grace\",\"songbook\":\"Hymns\",\"displayText\":\"42 - Amazing Grace\"}}"
}
```

> **Note:** The `payload` field is a JSON-encoded string (double-serialised), not a nested object.

---

## Typical flow with the song detail API

```
GET  /api/songs                          →  get catalog, find song number + songbook
GET  /api/songs/42?songbook=Hymns        →  get full lyrics/sections
POST /api/project  OR  /api/schedule/add →  send the song body above
```

