# Umbra

Rastreador de preços de jogos digitais e hardware para o varejo brasileiro.

## Visão do produto

O Umbra acompanha preços de jogos e de componentes de hardware em várias lojas, guarda o histórico de cada observação e avisa o usuário por email quando um produto que ele acompanha atinge o preço que ele definiu.

O acompanhamento é feito sobre o produto, não sobre a loja. O usuário informa quanto quer pagar por um jogo ou por uma peça, e o sistema observa todas as lojas rastreadas e responde onde está o menor preço. Quando mais de uma loja atinge o alvo no mesmo ciclo, o aviso lista todas, e a decisão de onde comprar fica com o usuário.

- Coleta agendada de preços, a cada 6 horas para jogos e a cada 12 horas para hardware.
- Histórico preservado sem sobrescrita, com gráfico e menor preço já registrado entre as lojas.
- Uma linha por jogo ou peça na busca, independentemente de quantas lojas o vendem.
- Aviso por email quando o preço alvo é atingido, no máximo um por produto a cada 24 horas.
- Conta com email e senha ou login com Discord.

Os preços vêm de três fontes, todas por API: IsThereAnyDeal, para jogos em reais em lojas como Steam, Nuuvem, GOG, Epic Games Store e GreenManGaming; a API da Steam; e o catálogo próprio da Kabum, para hardware.

## Stack

| Serviço | Tecnologias |
| :------ | :---------- |
| `umbra-api` | Java 25, Spring Boot 4.1.1, Spring Data JPA, Flyway, PostgreSQL 18, Resilience4j, Testcontainers |
| `umbra-web` | Angular 22, TypeScript |
