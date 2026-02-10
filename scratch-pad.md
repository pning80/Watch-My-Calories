curl "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions" \
-H "Content-Type: application/json" \
-H "Authorization: Bearer AIzaSyB09aXKQq2DtUck87_PHpgVkUjNQj1L5Oo" \
-d '{
  "model": "gemini-2.5-flash",
  "messages": [
    {
      "role": "user",
      "content": "Hello! Are you working?"
    }
  ]
}'