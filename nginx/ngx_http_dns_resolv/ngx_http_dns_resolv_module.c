#include <ngx_config.h>
#include <ngx_core.h>
#include <ngx_http.h>

typedef struct {
  ngx_http_complex_value_t   resolv_host;
} ngx_http_dns_resolv_loc_conf_t;

typedef struct {
  ngx_int_t           done;
  ngx_resolver_ctx_t *rdata;
} ngx_http_dns_resolv_ctx_t;

static void *ngx_http_dns_resolv_create_loc_conf(ngx_conf_t *cf);

static void ngx_http_dns_resolv_check_broken_connection(ngx_http_request_t *r);
static char *ngx_http_dns_resolv_host(ngx_conf_t *cf, ngx_command_t *cmd, void *conf);
static ngx_int_t ngx_http_dns_resolv_content_handler(ngx_http_request_t *r);

static ngx_http_module_t ngx_http_dns_resolv_module_ctx = {
  NULL,                                  /* preconfiguration */
  NULL,                                  /* postconfiguration */
  
  NULL,                                  /* create main configuration */
  NULL,                                  /* init main configuration */
  
  NULL,                                  /* create server configuration */
  NULL,                                  /* merge server configuration */
  
  ngx_http_dns_resolv_create_loc_conf,   /* create location configuration */
  NULL,                                  /* merge location configuration */  
};

static ngx_command_t ngx_http_dns_resolv_commands[] = {
  { ngx_string("dns_resolv_host"),
    NGX_HTTP_LOC_CONF|NGX_CONF_TAKE1,
    ngx_http_dns_resolv_host,
    NGX_HTTP_LOC_CONF_OFFSET,
    0,
    NULL },

  ngx_null_command
};

ngx_module_t ngx_http_dns_resolv_module = {
  NGX_MODULE_V1,
  &ngx_http_dns_resolv_module_ctx,  /* module context */
  ngx_http_dns_resolv_commands,     /* module directives */
  NGX_HTTP_MODULE,                  /* module type */
  NULL,                             /* init master */
  NULL,                             /* init module */
  NULL,                             /* init process */
  NULL,                             /* init thread */
  NULL,                             /* exit thread */
  NULL,                             /* exit process */
  NULL,                             /* exit master */
  NGX_MODULE_V1_PADDING
};

static void *
ngx_http_dns_resolv_create_loc_conf(ngx_conf_t *cf)
{
  ngx_http_dns_resolv_loc_conf_t *conf;
  conf = ngx_pcalloc(cf->pool, sizeof(ngx_http_dns_resolv_loc_conf_t));
  return conf;
}

static char *
ngx_http_dns_resolv_host(ngx_conf_t *cf, ngx_command_t *cmd, void *conf)
{
  ngx_http_dns_resolv_loc_conf_t *lcf = conf;
  ngx_http_core_loc_conf_t       *clcf;

  ngx_str_t                         *value;
  ngx_http_compile_complex_value_t   ccv;

  value = cf->args->elts;

  if (lcf->resolv_host.value.data) {
    return "is duplicate";
  }

  ngx_memzero(&ccv, sizeof(ngx_http_compile_complex_value_t));

  ccv.cf = cf;
  ccv.value = &value[1];
  ccv.complex_value = &lcf->resolv_host;

  if (ngx_http_compile_complex_value(&ccv) != NGX_OK) {
    return NGX_CONF_ERROR;
  }

  clcf = ngx_http_conf_get_module_loc_conf(cf, ngx_http_core_module);
  clcf->handler = ngx_http_dns_resolv_content_handler;

  return NGX_CONF_OK;
}

static ngx_str_t ngx_http_text_type = ngx_string("text/plain");

static void
ngx_http_dns_resolv_handler(ngx_resolver_ctx_t *ctx)
{
  ngx_http_request_t         *r, *p;
  ngx_uint_t                  rc, i;
  ngx_http_complex_value_t    cv;
  ngx_http_dns_resolv_ctx_t  *rctx;

  r = ctx->data;
  p = r->main;

  if (ctx->state) {
    ngx_log_error(NGX_LOG_ERR, r->connection->log, 0,
                  "%V could not be resolved (%i: %s)",
                  &ctx->name, ctx->state, ngx_resolver_strerror(ctx->state));
    rc = NGX_HTTP_INTERNAL_SERVER_ERROR;
    goto done;
  }

  ngx_memzero(&cv, sizeof(ngx_http_complex_value_t));
  cv.value.data = ngx_palloc(r->pool, ctx->naddrs * NGX_SOCKADDR_STRLEN);
  for (i = 0; i < ctx->naddrs; ++i) {
    cv.value.len += ngx_sock_ntop(ctx->addrs[i].sockaddr, ctx->addrs[i].socklen,
                                  cv.value.data + cv.value.len, NGX_SOCKADDR_STRLEN, 0);
    cv.value.data[cv.value.len++] = '\n';
  }
  rc = ngx_http_send_response(r, NGX_HTTP_OK, &ngx_http_text_type, &cv);

done:
  ngx_resolve_name_done(ctx);
  
  rctx = ngx_http_get_module_ctx(r, ngx_http_dns_resolv_module);
  if (rctx->rdata) {        /* if handler is called in ngx_resolver_process_* */
    rctx->rdata = 0;
    ngx_http_finalize_request(r, rc);
    if (p) ngx_http_run_posted_requests(p->connection);
  } else {                  /* if handler is called in ngx_resolve_name */
    rctx->done = 1;
  }
}

static void
ngx_http_dns_resolv_check_broken_connection(ngx_http_request_t *r)
{
  ngx_http_request_t        *p;
  ngx_http_dns_resolv_ctx_t *ctx;
  
  p = r->main;
  ctx = ngx_http_get_module_ctx(r, ngx_http_dns_resolv_module);
  
  if (ctx->rdata) ngx_resolve_name_done(ctx->rdata);
  ngx_http_finalize_request(r, NGX_HTTP_CLIENT_CLOSED_REQUEST);
  if (p) ngx_http_run_posted_requests(p->connection);
}

static void
ngx_http_dns_resolv_cleanup(void *data)
{
  ngx_http_dns_resolv_ctx_t *ctx;
  ctx = data;

  if (ctx->rdata) ngx_resolve_name_done(ctx->rdata);
}

static ngx_int_t
ngx_http_dns_resolv_content_handler(ngx_http_request_t *r)
{
  ngx_str_t                        host;
  ngx_resolver_ctx_t              *ctx, temp;
  ngx_http_core_loc_conf_t        *clcf;
  ngx_http_cleanup_t              *cln;
  ngx_http_dns_resolv_ctx_t       *rctx;
  ngx_http_dns_resolv_loc_conf_t  *lcf;

  lcf = ngx_http_get_module_loc_conf(r, ngx_http_dns_resolv_module);

  if (ngx_http_complex_value(r, &lcf->resolv_host, &host) != NGX_OK) {
    return NGX_HTTP_INTERNAL_SERVER_ERROR;
  }

  rctx = ngx_pcalloc(r->pool, sizeof(ngx_http_dns_resolv_ctx_t));
  if (!rctx) {
    return NGX_HTTP_INTERNAL_SERVER_ERROR;
  }
  ngx_http_set_ctx(r, rctx, ngx_http_dns_resolv_module);

  cln = ngx_http_cleanup_add(r, 0);
  if (cln == NULL) {
    return NGX_HTTP_INTERNAL_SERVER_ERROR;
  }

  /* cleanup resolver */
  cln->handler = ngx_http_dns_resolv_cleanup;
  cln->data    = rctx;  
  
  clcf = ngx_http_get_module_loc_conf(r, ngx_http_core_module);

  temp.name = host;
  
  ctx = ngx_resolve_start(clcf->resolver, &temp);
  if (ctx == NULL) {
    return NGX_HTTP_INTERNAL_SERVER_ERROR;
  }

  if (ctx == NGX_NO_RESOLVER) {
    ngx_log_error(NGX_LOG_ERR, r->connection->log, 0,
                  "no resolver defined to resolve %V", &host);
    return NGX_HTTP_BAD_GATEWAY;
  }

  ctx->name    = host;
  ctx->handler = ngx_http_dns_resolv_handler;
  ctx->data    = r;
  ctx->timeout = clcf->resolver_timeout;

  if (ngx_resolve_name(ctx) != NGX_OK) {
    return NGX_HTTP_INTERNAL_SERVER_ERROR;
  }

  /* if ctx->hanlder is called */
  if (rctx->done) return NGX_OK;

  rctx->rdata = ctx;
  r->read_event_handler = ngx_http_dns_resolv_check_broken_connection;
  r->write_event_handler = ngx_http_dns_resolv_check_broken_connection;

  r->main->count++;
  return NGX_DONE;
}

