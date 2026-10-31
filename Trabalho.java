import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

public class Trabalho {
    static void bubble(int[] v, int inicio, int fim) {
        for (int ultima = fim - 1; ultima > inicio; ultima--) {
            boolean trocou = false;
            for (int i = inicio; i < ultima; i++) {
                if (v[i] > v[i + 1]) {
                    trocar(v, i, i + 1);
                    trocou = true;
                }
            }
            if (!trocou) break;
        }
    }

    static void selection(int[] v, int inicio, int fim) {
        for (int i = inicio; i < fim - 1; i++) {
            int menor = i;
            for (int j = i + 1; j < fim; j++) {
                if (v[j] < v[menor]) menor = j;
            }
            trocar(v, i, menor);
        }
    }

    static void insertion(int[] v, int inicio, int fim) {
        for (int i = inicio + 1; i < fim; i++) {
            int valor = v[i];
            int j = i - 1;
            while (j >= inicio && v[j] > valor) {
                v[j + 1] = v[j];
                j--;
            }
            v[j + 1] = valor;
        }
    }

    static void merge(int[] v, int inicio, int fim) {
        if (fim - inicio < 2) return;
        int meio = (inicio + fim) / 2;
        merge(v, inicio, meio);
        merge(v, meio, fim);
        juntar(v, inicio, meio, fim);
    }

    static void juntar(int[] v, int inicio, int meio, int fim) {
        int[] auxiliar = new int[fim - inicio];
        int i = inicio;
        int j = meio;
        int k = 0;
        while (i < meio && j < fim) {
            if (v[i] <= v[j]) auxiliar[k++] = v[i++];
            else auxiliar[k++] = v[j++];
        }
        while (i < meio) auxiliar[k++] = v[i++];
        while (j < fim) auxiliar[k++] = v[j++];
        for (k = 0; k < auxiliar.length; k++) v[inicio + k] = auxiliar[k];
    }

    static void trocar(int[] v, int i, int j) {
        int temporario = v[i];
        v[i] = v[j];
        v[j] = temporario;
    }

    static void ordenar(int[] v, String algoritmo, int inicio, int fim) {
        switch (algoritmo) {
            case "Bubble": bubble(v, inicio, fim); break;
            case "Selection": selection(v, inicio, fim); break;
            case "Insertion": insertion(v, inicio, fim); break;
            case "Merge": merge(v, inicio, fim); break;
            default: throw new IllegalArgumentException("Algoritmo desconhecido");
        }
    }

    static void executar(int[] v, String algoritmo, int quantidade) throws InterruptedException {
        if (quantidade == 1) {
            ordenar(v, algoritmo, 0, v.length);
            return;
        }
        Thread[] threads = new Thread[quantidade];
        for (int t = 0; t < quantidade; t++) {
            final int inicio = t * v.length / quantidade;
            final int fim = (t + 1) * v.length / quantidade;
            threads[t] = new Thread(() -> ordenar(v, algoritmo, inicio, fim));
            threads[t].start();
        }
        for (Thread thread : threads) thread.join();
        for (int t = 1; t < quantidade; t++) {
            juntar(v, 0, t * v.length / quantidade, (t + 1) * v.length / quantidade);
        }
    }

    static int[] criarDados(int tamanho, String tipo, int limite, int amostra) {
        Random sorteio = new Random(2410454L + tamanho + limite + amostra);
        int[] v = new int[tamanho];
        for (int i = 0; i < tamanho; i++) v[i] = sorteio.nextInt(limite + 1);
        if (!tipo.equals("Aleatorio")) Arrays.sort(v);
        if (tipo.equals("Inverso")) {
            for (int i = 0; i < tamanho / 2; i++) trocar(v, i, tamanho - 1 - i);
        }
        return v;
    }

    public static void main(String[] args) throws Exception {
        String[] algoritmos = {"Bubble", "Selection", "Insertion", "Merge"};
        String[] tipos = {"Aleatorio", "Ordenado", "Inverso"};
        int[] tamanhos = {1000, 5000};
        int[] limites = {100, 100000};
        int[] quantidades = {1, 2, 4};
        int cpus = Runtime.getRuntime().availableProcessors();
        Path pasta = Path.of(args.length == 0 ? "meus_resultados" : args[0]);
        Files.createDirectory(pasta);
        Files.writeString(pasta.resolve("ambiente.txt"),
            "Java: " + System.getProperty("java.version") + "\nSistema: "
            + System.getProperty("os.name") + " " + System.getProperty("os.arch")
            + "\nCPUs visiveis: " + cpus + "\n");
        for (String algoritmo : algoritmos) {
            for (int quantidade : quantidades) {
                for (int i = 0; i < 5; i++) {
                    executar(criarDados(5000, "Aleatorio", 100000, i), algoritmo, quantidade);
                }
            }
        }
        try (BufferedWriter arquivo = Files.newBufferedWriter(pasta.resolve("tempos.csv"));
             BufferedWriter resumo = Files.newBufferedWriter(pasta.resolve("medias.csv"))) {
            String cabecalho = "algoritmo;tipo;tamanho;limite;threads;cpus;";
            arquivo.write(cabecalho + "amostra;tempo_ns\n");
            resumo.write(cabecalho + "media_ms;minimo_ms;maximo_ms\n");
            for (int tamanho : tamanhos) {
                for (String tipo : tipos) {
                    for (int limite : limites) {
                        for (String algoritmo : algoritmos) {
                            for (int quantidade : quantidades) {
                                double soma = 0;
                                double minimo = Double.MAX_VALUE;
                                double maximo = 0;
                                for (int amostra = 1; amostra <= 5; amostra++) {
                                    int[] dados = criarDados(tamanho, tipo, limite, amostra);
                                    int[] esperado = dados.clone();
                                    Arrays.sort(esperado);
                                    long inicio = System.nanoTime();
                                    executar(dados, algoritmo, quantidade);
                                    long tempo = System.nanoTime() - inicio;
                                    if (!Arrays.equals(dados, esperado)) {
                                        throw new IllegalStateException("Ordenacao incorreta");
                                    }
                                    double ms = tempo / 1000000.0;
                                    soma += ms;
                                    minimo = Math.min(minimo, ms);
                                    maximo = Math.max(maximo, ms);
                                    arquivo.write(algoritmo + ";" + tipo + ";" + tamanho + ";"
                                        + limite + ";" + quantidade + ";" + cpus + ";"
                                        + amostra + ";" + tempo + "\n");
                                }
                                resumo.write(algoritmo + ";" + tipo + ";" + tamanho + ";"
                                    + limite + ";" + quantidade + ";" + cpus + ";"
                                    + soma / 5 + ";" + minimo + ";" + maximo + "\n");
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Pronto! CSVs salvos em " + pasta);
    }
}