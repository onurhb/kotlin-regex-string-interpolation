# Regex Value Extractor
You have a text that you want to match using regex and create a string from the result:
```
    1992-01-13
```

Following regex will match above text:

``` regexp
    (?<year>\\d{4})-(?<month>\\d{2})(-(?<day>\\d{2}))?
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

Above code would produce following results:  
- `1992-01-13`: year: 1992, month: 01 and maybe day: 13
- `1992-01`: year: 1992, month: 01

# Usage
You need to create a template which tells `RegexValueExtractor` what to extract:

- Wrapping a substring with parentheses will prevent `RegexValueExtractor` from throwing exception if it can't find a group.
This will also ensure that everything inside the parentheses is erased. 
E.g. `{year}_{month}(_{day})` will remove the last portion `(_{day})` if `day` is not found.
- You can add a default if a group is not found, for example: `{year}_{month}_{day:01}` which will default day to `01` if not found
- `RegexValueExtractor` supports nesting parentheses 
- `RegexValueExtractor` supports multiple parameters if first is not found `{year}_{month}_{day|anotherday}` 
which will try to find `anotherday` if `day` is not found (can also default `anotherday:default0`)