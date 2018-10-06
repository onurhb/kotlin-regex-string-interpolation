# Regex Value Extractor
_Work in progress_  

#### Simple example
You have a text that you want to match using regex and create a string from the result:
``` js
    1992-01-13
```

Following regex will match above text:

``` regexp
    (?<year>\\d{4})-(?<month>\\d{2})-(?<day>\\d{2})?
```

This library helps you to easily extract values and also transform them:

``` kotlin
    fun extract() {
        // groupCollection is MatchGroupCollection from Regex match
        RegexValueExtractor.extractValues(
            template = "year: {year}, month: {month}( and maybe day: {day})", 
            groups = groupCollection
        )
    }
```

Above code would produce following results assuming that above regex was used:  
- `1992-01-13`: year: 1992, month: 01 and maybe day: 13
- `1992-01-`: year: 1992, month: 01
