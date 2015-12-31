package Test::BDD::Simple;

use strict;
use warnings;

use Data::Dumper;
use Test::More;
use JSON::PP qw(decode_json encode_json);
use MIME::Base64 qw(encode_base64 decode_base64);
use FindBin;
use LWP;
use Carp;

use Digest::SHA qw(hmac_sha256_base64);

our $DEBUG = 0;
our $DEBUG_HTTP = 0;
our $DEBUG_FUN = 0;

sub compile
{
    my $bddf  = shift;
    my $extra = shift || undef;

    open(my $bddfh, ">", $bddf) or die "can't open $bddf for write $!";
    
    while (my $line = <DATA>) {
        chomp $line;
        last if ($line eq "__END__");

        if ($line eq "__USEEND__") {
            if ($extra && -f $extra) {
                open IN, "<", $extra;
                local $/ = undef;
                print $bddfh <IN>;
                close(IN);
            }
            next;
        }

        print $bddfh $line, "\n";
    }

    close($bddfh);
}

sub new
{
    my ($class) = @_;
    return bless {save => {}, hrh => {}}, $class;
}

sub global
{
    my ($self, $global) = @_;
    $self->{global} = $global;
}

sub param
{
    my $self = shift;
    my %param = @_;

    $self->{param} = \%param;
}

sub expect
{
    my $self = shift;
    my %expect = @_;
    $self->{expect} = \%expect;
}

sub call
{
    my ($self) = @_;

    if (exists($self->{param}->{uri})) {
        $self->{param}->{uri} = $self->save_replace($self->{param}->{uri});
        $self->{param}->{uri} = $self->global_replace($self->{param}->{uri});
    }

    if (exists($self->{param}->{input}) && $self->{param}->{input}) {
        $self->{param}->{input} = $self->save_replace($self->{param}->{input});
        $self->{param}->{input} = $self->global_replace($self->{param}->{input});
        $self->{param}->{input} = $self->file_replace($self->{param}->{input});
    }

    if (exists($self->{expect}->{jsonmat}) && $self->{expect}->{jsonmat}) {
        $self->{expect}->{jsonmat} = $self->save_replace($self->{expect}->{jsonmat});
        $self->{expect}->{jsonmat} = $self->global_replace($self->{expect}->{jsonmat});
        $self->{expect}->{jsonmat} = $self->file_replace($self->{expect}->{jsonmat});
    }

    debug(Dumper($self));

    my $fun = {GET => "httpcall", PUT => "httpcall", DELETE => "httpcall",
               POST => "httpcall", Fun => "funcall",  HttpReqHeader => "hrhcall"
              }-> {$self->{param}->{method}};
    $self->$fun();
}

sub httpcall
{
    my ($self) = @_;

    my $agent = LWP::UserAgent->new();

    my $method = $self->{param}->{method};
    my $uri = $self->{param}->{uri};
    if (($method eq "GET" || $method eq "DELETE") &&
        $self->{param}->{input} ne "") {
        $uri .= $self->{param}->{input};
    } 
    my $request = HTTP::Request->new($method => $uri);
    if ($method eq "PUT" || $method eq "POST") {
        if ($self->{param}->{encode}) {
            $request->content_type('application/x-www-form-urlencoded')
                if ($self->{param}->{encode} eq "form");
        }
        if (length($self->{param}->{input}) == 0) {
            $request->header("Content-Length" => 0);
        } else {
            $request->content($self->{param}->{input});
        }
    }

    while (my ($k, $v) = each(%{$self->{hrh}})) {
        $request->header($k => $v);
    }

    my $response = $agent->request($request);

    debug_http(Dumper($request));
    debug_http(Dumper($response));

    $self->httpresponse({ code => $response->code,
                          body => $response->content });
}

sub funcall
{
    my ($self) = @_;

    my @params;
    while ($self->{param}->{input} =~ /'([^']+)'/g) {
        push(@params, $1);
    }

    my @saves;
    while ($self->{expect}->{funsave} =~ /'([^']+)'/g) {
        push(@saves, $1);
    }

    debug_fun(Dumper(@saves), $self->{param}->{fun}, Dumper(@params));

    my $fun = {
        base64_encode => sub {
            my ($s) = @_;
            (encode_base64($s));
        },
    }->{$self->{param}->{fun}};

    foreach (&$fun(@params)) {
        my $key = shift(@saves);
        $self->{save}->{$key} = $_;
    }
}

sub hrhcall
{
    my ($self) = @_;

    my $kv = $self->{param}->{input};
    if ($self->{param}->{encode}) {
        if ($self->{param}->{encode} eq "hmac_sha256_base64") {
            my $val = hmac_sha256_base64($kv->{val}, $self->{param}->{key});
            $val .= "=" while (length($val) % 4);
            print "Key ", $self->{param}->{key}, " Signature ", $val, "\n";
            
            $self->{hrh}->{$kv->{key}} = $val;
        }
    } else {
        $self->{hrh}->{$kv->{key}} = $kv->{val};        
    }
}

sub httpresponse
{
    my ($self, $return) = @_;
    is($return->{code}, $self->{expect}->{code}, "http code ok");

    if (exists $self->{expect}->{jsonmat}) {
        $self->jsonmat($return->{body}, $self->{expect}->{jsonmat});
    }
    
    if (exists $self->{expect}->{plaineq}) {
        is($return->{body}, $self->{expect}->{plaineq});
    }

    if ($self->{expect}->{jsonpp}) {
        my $obj = eval { decode_json($return->{body}) } or
            confess "malformed json ", $return->{body};
        print JSON::PP->new->pretty()->encode($obj);        
    }

    if (exists $self->{expect}->{jsonsave}) {
        $self->jsonsave($return->{body}, $self->{expect}->{jsonsave});
    }
}

sub jsonmat
{
    my ($self, $got, $expect) = @_;
    my $err = "$got match $expect";

    my $fun = sub {
        my ($gotval, $expectval) = @_;
        unless ($expectval eq '*') {
            is($gotval, $expectval, $err);
        }
    };

    jsonwalk(decode_json_safe($got), decode_json_safe($expect), $fun, $err);
}

sub jsonsave
{
    my ($self, $got, $expect) = @_;
    my $err = "$got match $expect";

    my $fun = sub {
        my ($gotval, $expectval) = @_;
        $self->{save}->{$expectval} = $gotval;
    };

    jsonwalk(decode_json_safe($got), decode_json_safe($expect), $fun, $err);
}

sub jsonwalk
{
    my ($got, $expect, $fun, $desc) = @_;

    is(ref($got), ref($expect), $desc);

    if (ref($expect) eq "HASH") {
        while (my ($key, $val) = each(%{$expect})) {
            ok(exists($got->{$key}), "$key found");
            jsonwalk($got->{$key}, $val, $fun, $desc);
        }
    } elsif (ref($expect) eq "ARRAY") {
        is(length(@{$got}), length(@{$expect}), $desc);
        for(my $i = 0; $i < @{$expect}; ++$i) {
            next if ($expect->[$i] eq '*');
            jsonwalk($got->[$i], $expect->[$i], $fun, $desc);
        }
    } else {
        &$fun($got, $expect);
    }
}

sub save_replace
{
    my ($self, $target) = @_;

    while ($target =~ /__(.+)__/g) {
        if (exists $self->{save}->{$1}) {
            my $save = $self->{save}->{$1};
            $target =~ s/__$1__/$save/g;
        }
    }
    return $target;
}

sub global_replace
{
    my ($self, $target) = @_;

    while ($target =~ /\{(.+)\}/g) {
        if (exists $self->{global}->{$1}) {
            my $save = $self->{global}->{$1};
            $target =~ s/\{$1\}/$save/g;
        }
    }
    return $target;
}

sub file_replace
{
    my ($self, $target) = @_;
    while ($target =~ m/\@([a-zA-Z0-9._\/-]+)/g) {
        my $f = (substr($1, 0, 1) eq '/') ? $1 : "$FindBin::Bin/$1";
        if (-f $f) {
            my $content = loadfile($f);
            $target =~ s/\@$1/$content/g;
        }
    }
    return $target;
}

sub loadfile
{
    my $f = shift;
    open IN, "<", $f;
    local $/ = undef;
    my $c = <IN>;
    close IN;
    return $c;
}

sub decode_json_safe
{
    my $json = shift;
    my $obj = eval { decode_json($json) };
    die "malformed JSON $json" unless ($obj);
    return $obj;
}

sub debug_http { print @_, "\n" if $DEBUG_HTTP; }
sub debug_fun { print @_, "\n" if $DEBUG_FUN; }
sub debug { print @_, "\n" if $DEBUG };

1;

__DATA__
#!/usr/bin/perl

use strict;
use warnings;

use Test::BDD::Simple;
use Test::More;
use Method::Signatures;

__USEEND__

Given qr/.+/, func($c) {
    my $row = $c->data ? @{$c->data}[0] : {};

    my $obj = Test::BDD::Simple->new();
    ok($obj, "Test::BDD::Simple created");
    $c->stash->{scenario}->{object} = $obj;
    $obj->global($row);
};

When qr/HttpReqHeader\s+([^\s]+)\s+With\s+'(.+)'$/, func($c) {
    my ($name, $value) = ($1, $2);
    $c->stash->{scenario}->{object}->param(method => "HttpReqHeader",
                                           input => {key => $name, val => $value});
};

When qr/HttpReqHeader\s+([^\s]+)\s+HmacSha256Base64\s+'([^']+)'\s+'(.+)'$/, func($c) {
    my ($name, $key, $value) = ($1, $2, $3);
    $c->stash->{scenario}->{object}->param(method => "HttpReqHeader",
                                           encode => "hmac_sha256_base64",
                                           key    => $key,
                                           input  => {key => $name, val => $value});
};


Then qr/HttpReqHeaderSave/, func($c) {
    $c->stash->{scenario}->{object}->call();
};

When qr/(GET|DELETE|PUT|POST)\s+([^\s]+)\s+With(:(.+))?\s+'(.+)'$/, func($c) {
    my ($method, $uri, $encode, $input) = ($1, $2, $4, $5);
    $c->stash->{scenario}->{object}->param(method => $method,
                                           uri => $uri,
                                           encode => $encode,
                                           input => $input);
};

When qr/(GET|DELETE|PUT|POST)\s+([^\s]+)\s+With(:(.+))?$/, func($c) {
    my ($method, $uri, $encode, $input) = ($1, $2, $4, $c->data);
    $c->stash->{scenario}->{object}->param(method => $method,
                                           uri => $uri,
                                           encode => $encode,
                                           input => $input);
};

When qr/(GET|DELETE|PUT|POST)\s+([^\s]+)$/, func($c) {
    my ($method, $uri) = ($1, $2);
    $c->stash->{scenario}->{object}->param(method => $method,
                                           uri => $uri,
                                           encode => undef,
                                           input => "");
};

Then qr/HttpCode\s+(\d+)\s+JsonMatch\s+'(.+)'\s+JsonSave\s+'(.+)'$/, func($c) {
    my ($code, $jsonmat, $jsonsave) = ($1, $2, $3);
    $c->stash->{scenario}->{object}->expect(code => $code,
                                            jsonmat => $jsonmat,
                                            jsonsave => $jsonsave);
    $c->stash->{scenario}->{object}->call();
};

Then qr/HttpCode\s+(\d+)\s+JsonMatch\s+'(.+)'$/, func($c) {
    my ($code, $json) = ($1, $2);
    $c->stash->{scenario}->{object}->expect(code => $code,
                                            jsonmat => $json);
    $c->stash->{scenario}->{object}->call();
};

Then qr/HttpCode\s+(\d+)\s+JsonPrint$/, func($c) {
    my ($code) = ($1);
    $c->stash->{scenario}->{object}->expect(code   => $code,
                                            jsonpp => 1);
    $c->stash->{scenario}->{object}->call();
};

Then qr/HttpCode\s+(\d+)\s+PlainEqual\s+'(.+)'$/, func($c) {
    my ($code, $plain) = ($1, $2);
    $c->stash->{scenario}->{object}->expect(code    => $code,
                                            plaineq => $plain);
    $c->stash->{scenario}->{object}->call();
};

Then qr/HttpCode\s+(\d+)$/, func($c) {
    my $code = $1;
    $c->stash->{scenario}->{object}->expect(code => $code);
    $c->stash->{scenario}->{object}->call();
};

When qr/Fun\s+([^\s]+)\s+With\s+\((.+)\)$/, func($c) {
    my ($fun, $input) = ($1, $2);
    $c->stash->{scenario}->{object}->param(method => 'Fun',
                                           fun   => $fun,
                                           input => $input);
};

Then qr/FunSave\s+\((.+)\)$/, func($c) {
    my ($save) = ($1);
    $c->stash->{scenario}->{object}->expect(funsave => $save);
    $c->stash->{scenario}->{object}->call();
};

__END__
