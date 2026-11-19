# StockAi

**Aplicativo Android para mapeamento 3D e localização de itens em estoque.**

Projeto desenvolvido para o 6º Workshop de Educação Continuada em TI (28 a 30 de setembro de 2026), no Curso Superior de Tecnologia em Análise e Desenvolvimento de Sistemas da UNICID – Universidade Cidade de São Paulo.

## Sobre o projeto

Em estoques, almoxarifados e depósitos, é comum perder tempo procurando itens guardados em locais desconhecidos ou mal identificados, o que gera atrasos, retrabalho e compras duplicadas.

O StockAi transforma o estoque em um mapa visual. O usuário cadastra ambientes com as medidas reais, posiciona prateleiras na planta, nomeia as gôndolas e cataloga cada item no local exato onde está guardado. Uma busca indica em segundos o ambiente, a prateleira e a gôndola do item, com destaque visual na planta 2D e na visualização 3D.

O app funciona **100% offline**: os dados ficam no próprio aparelho e a cena 3D é desenhada pela GPU do celular.

## Funcionalidades

- **Conta de usuário:** cadastro e login, com senha protegida por hash PBKDF2 e sessão salva
- **Ambientes:** cadastro com largura e comprimento em metros, edição e exclusão em cascata
- **Planta 2D em escala:** grade de 1 metro, prateleiras arrastáveis, rotação de 90° e bloqueio de sobreposição
- **Prateleiras e gôndolas:** vista frontal com os níveis empilhados e códigos sugeridos automaticamente (B1, B2, B3...)
- **Itens:** cadastro com quantidade e unidade, ajuste rápido com botões + e −, e data da última atualização
- **Busca:** resultados enquanto se digita, caminho completo do item, buscas recentes e destaque do local na planta
- **Visualização 3D:** ambiente montado a partir dos dados cadastrados, com órbita e zoom por gestos e marcador no item buscado
- **Dados de exemplo:** cria com um toque um almoxarifado completo para demonstração

## Tecnologias

| Tecnologia | Uso no projeto |
| --- | --- |
| Kotlin | Linguagem de todo o app |
| Android Studio | Ambiente de desenvolvimento |
| Jetpack Compose + Material 3 | Interface, componentes e tema |
| Room (SQLite) + KSP | Banco de dados local |
| SceneView 2.3.3 (Filament) | Visualização 3D |
| Compose Canvas | Planta 2D e vista frontal das prateleiras |
| Navigation Compose | Navegação entre telas |
| ViewModel + Kotlin Flow | Arquitetura MVVM e telas reativas |
| PBKDF2 (javax.crypto) | Hash das senhas |

## Requisitos

- Android Studio com suporte a AGP 9.4 ou superior
- JDK 21 (já incluso no Android Studio)
- Aparelho ou emulador com **Android 8.0 (API 26)** ou superior
- Suporte a **OpenGL ES 3.0** para a visualização 3D (sem ele, o app mostra um aviso e a planta 2D continua funcionando)

Principais versões configuradas no projeto:

| Item | Versão |
| --- | --- |
| Android Gradle Plugin | 9.4.1 |
| Kotlin | 2.2.10 |
| compileSdk / targetSdk | 37 |
| minSdk | 26 |
| Java (compileOptions) | 21 |
| Compose BOM | 2026.02.01 |
| Room | 2.8.5 |
| KSP | 2.3.8 |
| Navigation Compose | 2.9.7 |
| SceneView | 2.3.3 |

## Como executar

1. Clone o repositório ou copie a pasta do projeto
2. Abra a pasta no Android Studio (**File › Open**)
3. Aguarde o **Gradle Sync** terminar (na primeira vez, baixa o SceneView e o motor Filament)
4. Crie ou selecione um emulador no **Device Manager**, ou conecte um celular com a **Depuração USB** ativada
5. Clique em **Run ▶**
6. Crie uma conta e, em **Meus ambientes**, toque nos três pontinhos › **Criar ambiente de exemplo** para explorar o app com dados prontos

## Estrutura do projeto

```text
com.stockai.app
|-- data
|   |-- local                -> Room: entidades, DAOs, consultas e banco
|   `-- repository           -> repositorios, autenticacao, buscas recentes e dados de exemplo
|-- ui
|   |-- components           -> componentes reutilizaveis (barra superior, logo, campo de senha)
|   |-- navigation           -> rotas e NavHost
|   |-- screens
|   |   |-- login            -> entrada
|   |   |-- cadastro         -> criacao de conta
|   |   |-- ambientes        -> lista "Meus ambientes"
|   |   |-- ambiente         -> planta 2D, vista 3D e prateleiras
|   |   |-- prateleira       -> vista frontal e gondolas
|   |   |-- gondola          -> itens da gondola
|   |   `-- busca            -> busca de itens
|   |-- theme                -> cores e tema Material 3
|   `-- AppViewModelProvider.kt
|-- MainActivity.kt
`-- StockAiApp.kt            -> container de dependencias
```

## Arquitetura

O app segue o padrão **MVVM**:

- **View** (telas em Compose) mostra o estado e repassa as ações do usuário
- **ViewModel** guarda o estado da tela em `StateFlow` e aplica as regras (validações, sobreposição, busca)
- **Repository** é a única porta de acesso aos dados
- **Room** persiste tudo localmente; consultas que devolvem `Flow` atualizam as telas automaticamente

### Modelo de dados

```text
Usuario --< Ambiente --< Prateleira --< Gondola --< Item
```

Cada nível aponta para o de cima por chave estrangeira com exclusão em cascata. Todas as medidas e posições são guardadas em **metros**; a planta 2D e a cena 3D convertem esses valores para a escala da tela.

## Solução de problemas

**O app fecha ao abrir a aba 3D no emulador.** O emulador não está entregando OpenGL ES 3.0. No Device Manager, edite o dispositivo e deixe **Graphics** como **Hardware**; nos Extended Controls (**⋮ › Settings › Advanced**), escolha **OpenGL ES API level: Renderer maximum**. Depois, use **Cold Boot Now**. Em celular físico, o problema não costuma ocorrer.

**Botão Run desativado.** Provavelmente há um Gradle Sync pendente; clique em **Sync Now** na faixa azul do editor.

**O ícone novo não aparece.** Desinstale o app do aparelho e execute novamente.

## Autores

- Francisco Cardeal Oliveira (RGM 47484772)
- Ronan Camilo Gomes (RGM 40683826)
- David Freddy Maldonado Paxi (RGM 43489672)

## Referências

- ANDROID DEVELOPERS. [Jetpack Compose](https://developer.android.com/jetpack/compose)
- ANDROID DEVELOPERS. [Salvar dados em um banco de dados local usando o Room](https://developer.android.com/training/data-storage/room)
- JETBRAINS. [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- SCENEVIEW. [SceneView for Android](https://github.com/sceneview/sceneview)
