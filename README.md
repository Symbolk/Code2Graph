# Code2Graph: Convert source code into graph.

> With lightweight static analysis on nesting hierarchy, control and data flow.

> For various downstream tasks in software&language engineering:
> - Software architecture analysis
> - Semantic code diff
> - Semantic code merge
> - Impact analysis on code change
> - Co-change prediction at different granularity

## Highlights: 

- Support multi-language code, easy to extend
- Support cross-lang link, specific to framework
- Submodule architecture, flexible combination of downstream/high-level tasks

![architecture](/images/architecture.png?raw=true "architecture")

## Language Support

- Java (done): Eclipse JDT Parser
- XML (done): SAXParser
- Kotlin (doing): kotlinx
- Python (in plan)
- JavaScript (in plan)

> Special thanks to [Gumtree], which has a nice design for language-agnostic abstraction.  

[Gumtree]: https://github.com/Symbolk/SmartCommit/releases

## Usage
### CLI

### API (Java)


### I/O 

### Input

### Output

#### Data Persistence
- Neo4j
- GraphViz dot file

## Design

### Graph Schema

- Node

- Edge