import groovy.json.JsonSlurper
import groovy.json.JsonOutput

// This is the listener handler that will be triggered when an issue is updated
def event = binding.variables['event'] // The event triggered for the issue update
def issue = event.issue // The updated issue

// Get the sentiment value from custom field 100054
def sentiment = issue.fields.customfield_10054 // Adjust if necessary
if (!sentiment) {
    println("Sentiment not found in custom field 100054.")
    return
}

println("Sentiment: ${sentiment}")

def issueId = issue.id
println("Issue ID: ${issueId}")
println("Setting emotions history")

// Fetch the current emotion log for the issue
def emotionLogResponse = Unirest.get("/rest/api/3/issue/${issue.key}/properties/emotionLog").asJson()
println(emotionLogResponse)
if (emotionLogResponse.getStatus() == 404) {
    // If no emotionLog exists, create a new one
    def newEmotionLog = [
        issues: [
            [
                issueID: issueId,
                properties: [
                    emotionLog: [(System.currentTimeMillis().toString()): sentiment]
                ]
            ]
        ]
    ]
    def bodyData = JsonOutput.toJson(newEmotionLog)
    // Use Unirest to make a POST request with the correct syntax
    def response = Unirest.post("/rest/api/3/issue/properties/multi")
        .header("Content-Type", "application/json")
        .body(bodyData)
        .asJson()
    println("Emotion log created")
} else if (emotionLogResponse.getStatus() == 200) {
    // Parse the existing emotionLog data
    def propData = new JsonSlurper().parseText(emotionLogResponse.body.toString())
    def propValue = propData.value ?: [:]

    // Add the new sentiment value to the existing emotion log
    propValue[System.currentTimeMillis().toString()] = sentiment

    // Construct the request body
    def updatedEmotionLog = [
        issues: [
            [
                issueID: issueId,
                properties: [
                    emotionLog: [(System.currentTimeMillis().toString()): sentiment] + propValue
                ]
            ]
        ]
    ]
    def bodyData = JsonOutput.toJson(updatedEmotionLog)
    // Use Unirest to make a POST request with the correct syntax
    def response = Unirest.post("/rest/api/3/issue/properties/multi")
        .header("Content-Type", "application/json")
        .body(bodyData)
        .asJson()
    println("Emotion log update")
} else {
    println("Failed to fetch emotion log")
}
