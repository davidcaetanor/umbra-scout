import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiConfiguration } from '../../../api/api-configuration';
import { BuscarProdutos$Params, buscarProdutos } from '../../../api/fn/catalogo/buscar-produtos';
import { destaquesDoCatalogo } from '../../../api/fn/catalogo/destaques-do-catalogo';
import { BuscaProdutosResponse } from '../../../api/models/busca-produtos-response';
import { ProdutoExternoResponse } from '../../../api/models/produto-externo-response';
import { ProdutoResumoResponse } from '../../../api/models/produto-resumo-response';
import { CategoriaDeHardware, FiltroDaBusca } from './filtro-da-busca';
import { capaPelaOrigem } from './imagem-pela-origem';
import { precoDaOferta } from './preco-da-oferta';
import { ProdutoDoCatalogo } from './produto-do-catalogo';
import { ProdutoExterno } from './produto-externo';
import { ResultadoDaBusca } from './resultado-da-busca';
import { ROTULO_DA_CATEGORIA } from './rotulos';

@Injectable({ providedIn: 'root' })
export class CatalogoService {
  private readonly http = inject(HttpClient);
  private readonly configuracao = inject(ApiConfiguration);

  buscar(filtro: FiltroDaBusca): Observable<ResultadoDaBusca> {
    return this.chamar(paraParametros(filtro)).pipe(map(paraResultado));
  }

  buscarForaDoCatalogo(filtro: FiltroDaBusca): Observable<readonly ProdutoExterno[]> {
    return this.chamar({ ...paraParametros({ ...filtro, pagina: 0 }), incluirExterno: true }).pipe(
      map((resposta) => resposta.externos.map(paraProdutoExterno)),
    );
  }

  destaques(): Observable<readonly ProdutoDoCatalogo[]> {
    return destaquesDoCatalogo(this.http, this.configuracao.rootUrl).pipe(
      map((resposta) => resposta.body.conteudo.map(paraProduto)),
    );
  }

  private chamar(parametros: BuscarProdutos$Params): Observable<BuscaProdutosResponse> {
    return buscarProdutos(this.http, this.configuracao.rootUrl, parametros).pipe(map((resposta) => resposta.body));
  }
}

const CENTAVOS_POR_REAL = 100;

function paraParametros(filtro: FiltroDaBusca): BuscarProdutos$Params {
  return {
    ...(filtro.termo === null ? {} : { nome: filtro.termo }),
    ...(filtro.tipo === null ? {} : { tipo: filtro.tipo }),
    ...(filtro.categoria === null ? {} : { categoria: filtro.categoria }),
    ordenacao: filtro.ordenacao,
    ...(filtro.precoMin === null ? {} : { precoMin: filtro.precoMin * CENTAVOS_POR_REAL }),
    ...(filtro.precoMax === null ? {} : { precoMax: filtro.precoMax * CENTAVOS_POR_REAL }),
    ...(filtro.descontoMin === null ? {} : { descontoMin: filtro.descontoMin }),
    ...(filtro.loja === null ? {} : { loja: filtro.loja }),
    page: filtro.pagina,
  };
}

function paraResultado(resposta: BuscaProdutosResponse): ResultadoDaBusca {
  return {
    produtos: resposta.conteudo.map(paraProduto),
    pagina: resposta.pagina,
    totalDePaginas: resposta.totalPaginas,
    total: resposta.totalElementos,
  };
}

function paraProduto(produto: ProdutoResumoResponse): ProdutoDoCatalogo {
  return {
    id: produto.id,
    nome: produto.nome,
    tipo: produto.tipo,
    capa: capaPelaOrigem(produto.imagemUrl),
    categoria: produto.categoria ? (ROTULO_DA_CATEGORIA[produto.categoria as CategoriaDeHardware] ?? null) : null,
    preco: produto.melhorPreco ? precoDaOferta(produto.melhorPreco) : null,
  };
}

function paraProdutoExterno(produto: ProdutoExternoResponse): ProdutoExterno {
  return {
    gid: produto.gid,
    nome: produto.nome,
    capa: capaPelaOrigem(produto.imagemUrl),
    preco: precoDaOferta(produto.melhorPreco),
    enderecoDaLoja: produto.melhorPreco.url,
  };
}
