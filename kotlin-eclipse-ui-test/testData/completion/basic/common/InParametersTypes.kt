class SomeClass {
  class SomeInternal

  fun some(a : S<caret>)
}

// INVOCATION_COUNT: 1
// EXIST: SomeClass
// EXIST: SomeInternal
// EXIST: { lookupString:"String", tailText:" (jet)" }
// EXIST: StringBuilder
// EXIST_JAVA_ONLY: StringBuffer
// ABSENT: HTMLStyleElement